package ru.practicum.explorewithme.comments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.comments.dto.CommentDto;
import ru.practicum.explorewithme.comments.dto.ShortCommentDto;
import ru.practicum.explorewithme.comments.mapper.CommentMapper;
import ru.practicum.explorewithme.comments.model.Comment;
import ru.practicum.explorewithme.comments.repository.CommentRepository;
import ru.practicum.explorewithme.dto.error.ConditionsNotMetException;
import ru.practicum.explorewithme.dto.error.NotFoundException;
import ru.practicum.explorewithme.event.client.EventClient;
import ru.practicum.explorewithme.event.dto.EventForRequestDto;
import ru.practicum.explorewithme.event.model.EventState;
import ru.practicum.explorewithme.user.client.UserClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final UserClient userClient;
    private final EventClient eventClient;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, ShortCommentDto dto) {
        validateUserExists(userId);
        EventForRequestDto event = getEvent(eventId);
        if (!EventState.PUBLISHED.name().equals(event.getState())) {
            throw new ConditionsNotMetException("Event must be published to add comments");
        }
        Comment comment = CommentMapper.toComment(dto, userId, eventId);
        Comment saved = commentRepository.save(comment);
        return CommentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, ShortCommentDto dto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> commentNotFound(commentId));
        if (!comment.getAuthorId().equals(userId)) {
            throw new ConditionsNotMetException("Only the author of the comment can update it.");
        }
        comment.setText(dto.getText());
        comment.setUpdated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        return CommentMapper.toDto(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> commentNotFound(commentId));
        if (!comment.getAuthorId().equals(userId)) {
            throw new ConditionsNotMetException("Only the author of the comment can delete it.");
        }
        commentRepository.delete(comment);
    }

    @Override
    public CommentDto getComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> commentNotFound(commentId));
        return CommentMapper.toDto(comment);
    }

    @Override
    public List<CommentDto> getCommentsByEventId(Long eventId, int from, int size) {
        getEvent(eventId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "created"));
        return commentRepository.findAllByEventId(eventId, pageable)
                .stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> commentNotFound(commentId));
        commentRepository.delete(comment);
    }

    private NotFoundException commentNotFound(Long commentId) {
        return new NotFoundException("Comment with id=" + commentId + " was not found");
    }

    private void validateUserExists(long userId) {
        userClient.getUser(userId);
    }

    private EventForRequestDto getEvent(long eventId) {
        return eventClient.getEventForRequest(eventId);
    }
}
