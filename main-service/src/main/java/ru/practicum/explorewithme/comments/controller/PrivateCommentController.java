package ru.practicum.explorewithme.comments.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.comments.dto.CommentDto;
import ru.practicum.explorewithme.comments.dto.ShortCommentDto;
import ru.practicum.explorewithme.comments.service.CommentService;

@Slf4j
@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class PrivateCommentController {

    private final CommentService commentService;

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable @NotNull Long userId,
                                 @PathVariable @NotNull Long eventId,
                                 @RequestBody @Valid ShortCommentDto dto) {

        log.info("Adding comment for user {} and event {}", userId, eventId);
        return commentService.addComment(userId, eventId, dto);
    }

    @PatchMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto updateComment(@PathVariable @NotNull Long userId,
                                    @PathVariable @NotNull Long commentId,
                                    @RequestBody @Valid ShortCommentDto dto) {
        log.info("Updating comment for user {} and comment {}", userId, commentId);
        return commentService.updateComment(userId, commentId, dto);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable @NotNull Long userId,
                              @PathVariable @NotNull Long commentId) {
        log.info("Deleting comment for user {} comment {}", userId, commentId);
        commentService.deleteComment(userId, commentId);
    }
}
