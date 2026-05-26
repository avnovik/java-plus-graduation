package ru.practicum.explorewithme.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.dto.error.NotFoundException;
import ru.practicum.explorewithme.event.dto.EventForRequestDto;
import ru.practicum.explorewithme.event.mapper.EventMapper;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.event.repository.EventRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalEventServiceImpl implements InternalEventService {

    private final EventRepository eventRepository;

    @Override
    public EventForRequestDto getEventForRequest(long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        return EventMapper.toEventForRequestDto(event);
    }
}
