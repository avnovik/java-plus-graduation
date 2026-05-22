package ru.practicum.explorewithme.comments.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.comments.dto.CommentDto;
import ru.practicum.explorewithme.comments.dto.ShortCommentDto;
import ru.practicum.explorewithme.comments.model.Comment;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.user.model.User;

@UtilityClass
public class CommentMapper {

    public static Comment toComment(ShortCommentDto commentDto, User author, Event event) {
        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setAuthor(author);
        comment.setEvent(event);
        return comment;
    }

    public static CommentDto toDto(Comment comment) {
        return  new CommentDto(comment.getId(),
                comment.getEvent().getId(),
                comment.getAuthor().getId(),
                comment.getText(),
                comment.getCreated(),
                comment.getUpdated());
    }
}
