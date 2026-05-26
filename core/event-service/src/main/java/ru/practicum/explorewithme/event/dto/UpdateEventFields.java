package ru.practicum.explorewithme.event.dto;

import java.time.LocalDateTime;

public interface UpdateEventFields {
    String getAnnotation();

    Long getCategory();

    String getDescription();

    LocalDateTime getEventDate();

    LocationDto getLocation();

    Boolean getPaid();

    Integer getParticipantLimit();

    Boolean getRequestModeration();

    String getTitle();
}
