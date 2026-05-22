package ru.practicum.explorewithme.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.event.dto.EventFullDto;
import ru.practicum.explorewithme.event.dto.EventShortDto;
import ru.practicum.explorewithme.event.dto.NewEventDto;
import ru.practicum.explorewithme.event.dto.UpdateEventUserRequest;
import ru.practicum.explorewithme.event.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable long userId, @RequestBody @Valid NewEventDto newEventDto) {
        log.debug("POST /users/{}/events: title='{}', category={}, eventDate={}",
                userId,
                newEventDto.getTitle(),
                newEventDto.getCategory(),
                newEventDto.getEventDate());
        return eventService.addEvent(userId, newEventDto);
    }

    @GetMapping
    public List<EventShortDto> getEvents(@PathVariable long userId,
                                         @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                         @RequestParam(defaultValue = "10") @Positive int size) {
        log.debug("GET /users/{}/events: from={}, size={}", userId, from, size);
        return eventService.getEvents(userId, from, size);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEventByIdToUser(@PathVariable @NotNull Long userId,
                                           @PathVariable @NotNull Long eventId) {
        log.debug("GET /users/{}/events/{}", userId, eventId);
        return eventService.getEventByIdToUser(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@PathVariable @NotNull Long userId,
                                    @PathVariable @NotNull Long eventId,
                                    @RequestBody(required = false) @Valid UpdateEventUserRequest requestDto) {
        log.debug("Patch /users/{}/events/{}", userId, eventId);
        return eventService.updateEventByIdToUser(userId, eventId, requestDto);
    }

}
