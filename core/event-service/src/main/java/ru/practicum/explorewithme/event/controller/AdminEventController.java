package ru.practicum.explorewithme.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.event.dto.EventAdminSettingSearchDto;
import ru.practicum.explorewithme.event.dto.EventFullDto;
import ru.practicum.explorewithme.event.dto.UpdateEventAdminRequest;
import ru.practicum.explorewithme.event.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> findAllEvents(@ModelAttribute @Valid EventAdminSettingSearchDto settingSearchDto) {
        log.info("AdminEventController.findAllEvents()");
        return eventService.findAllEventsToAdmin(settingSearchDto);
    }

    @PatchMapping("{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEventById(@PathVariable Long eventId,
                                        @RequestBody(required = false) @Valid UpdateEventAdminRequest adminRequest) {
        log.info("AdminEventController.findEventById()");
        return eventService.updateEventByIdToAdmin(eventId, adminRequest);
    }
}
