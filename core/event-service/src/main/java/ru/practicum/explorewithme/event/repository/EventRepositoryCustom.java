package ru.practicum.explorewithme.event.repository;

import ru.practicum.explorewithme.event.dto.EventAdminSettingSearchDto;
import ru.practicum.explorewithme.event.model.Event;

import java.time.LocalDateTime;
import java.util.List;


public interface EventRepositoryCustom {

    List<Event> findEventsToAdmin(EventAdminSettingSearchDto settingSearch);

    List<Event> findPublicEvents(String text,
                                List<Long> categories,
                                Boolean paid,
                                LocalDateTime rangeStart,
                                LocalDateTime rangeEnd,
                                Boolean onlyAvailable,
                                Integer from,
                                Integer size,
                                boolean sortByEventDate);
}
