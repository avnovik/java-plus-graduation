package ru.practicum.explorewithme.comments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {
    private Long id;
    private Long eventId;
    private Long authorId;
    private String text;
    private LocalDateTime created;
    private LocalDateTime updated;
}
