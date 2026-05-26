package ru.practicum.explorewithme.comments.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.comments.dto.CommentDto;
import ru.practicum.explorewithme.comments.dto.ShortCommentDto;
import ru.practicum.explorewithme.comments.model.Comment;

@UtilityClass
public class CommentMapper {

    public static Comment toComment(ShortCommentDto commentDto, Long authorId, Long eventId) {
        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setAuthorId(authorId);
        comment.setEventId(eventId);
        return comment;
    }

    public static CommentDto toDto(Comment comment) {
        return  new CommentDto(comment.getId(),
                comment.getEventId(),
                comment.getAuthorId(),
                comment.getText(),
                comment.getCreated(),
                comment.getUpdated());
    }
}
