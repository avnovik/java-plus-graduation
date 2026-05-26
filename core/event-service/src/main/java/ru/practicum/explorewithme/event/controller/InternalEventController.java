package ru.practicum.explorewithme.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.event.dto.EventForRequestDto;
import ru.practicum.explorewithme.event.service.InternalEventService;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final InternalEventService internalEventService;

    @GetMapping("/{eventId}/for-request")
    public EventForRequestDto getEventForRequest(@PathVariable long eventId) {
        return internalEventService.getEventForRequest(eventId);
    }
}
