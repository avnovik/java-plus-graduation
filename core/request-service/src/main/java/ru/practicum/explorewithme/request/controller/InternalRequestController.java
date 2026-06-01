package ru.practicum.explorewithme.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.request.dto.EventConfirmedCountDto;
import ru.practicum.explorewithme.request.service.InternalRequestService;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final InternalRequestService internalRequestService;

    @GetMapping("/confirmed-count")
    public List<EventConfirmedCountDto> getConfirmedCounts(@RequestParam List<Long> eventIds) {
        return internalRequestService.getConfirmedCounts(eventIds);
    }

    @GetMapping("/confirmed")
    public boolean hasConfirmedRequest(@RequestParam Long userId,
                                       @RequestParam Long eventId) {
        return internalRequestService.hasConfirmedRequest(userId, eventId);
    }
}
