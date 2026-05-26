package ru.practicum.explorewithme.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventForRequestDto {
    private Long id;
    private Long initiatorId;
    private boolean requestModeration;
    private int participantLimit;
    private String state;
}
