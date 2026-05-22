package ru.practicum.explorewithme.comments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShortCommentDto {
    private Long id;
    @NotBlank
    @Size(min = 1, max = 2000)
    private String text;
}
