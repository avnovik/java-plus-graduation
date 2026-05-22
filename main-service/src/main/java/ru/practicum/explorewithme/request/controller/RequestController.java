package ru.practicum.explorewithme.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.request.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.request.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto create(
            @PathVariable Long userId,
            @RequestParam Long eventId
    ) {
        return service.create(userId, eventId);
    }

    @GetMapping
    public List<ParticipationRequestDto> getUserRequests(
            @PathVariable Long userId
    ) {
        return service.getUserRequests(userId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancel(
            @PathVariable Long userId,
            @PathVariable Long requestId
    ) {
        return service.cancel(userId, requestId);
    }
}
