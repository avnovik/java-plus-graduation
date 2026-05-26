package ru.practicum.explorewithme.event.service;

import ru.practicum.explorewithme.event.dto.EventForRequestDto;

public interface InternalEventService {

    EventForRequestDto getEventForRequest(long eventId);
}
