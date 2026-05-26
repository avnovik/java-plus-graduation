package ru.practicum.explorewithme.event.service;

import ru.practicum.explorewithme.event.dto.*;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    EventFullDto addEvent(long userId, NewEventDto dto);

    List<EventShortDto> getEvents(long userId, int from, int size);

    List<EventShortDto> getPublicEvents(String text,
                                        List<Long> categories,
                                        Boolean paid,
                                        LocalDateTime rangeStart,
                                        LocalDateTime rangeEnd,
                                        Boolean onlyAvailable,
                                        PublicEventSort sort,
                                        int from,
                                        int size);

    EventFullDto getPublicEventById(Long eventId);

    List<EventFullDto> findAllEventsToAdmin(EventAdminSettingSearchDto settingSearchDto);

    EventFullDto updateEventByIdToAdmin(Long eventId, UpdateEventAdminRequest adminRequest);

    EventFullDto getEventByIdToUser(Long userId, Long eventId);

    EventFullDto updateEventByIdToUser(Long userId, Long eventId, UpdateEventUserRequest requestDto);
}
