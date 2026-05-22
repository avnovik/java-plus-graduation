package ru.practicum.explorewithme.compilations.repository;

import ru.practicum.explorewithme.event.model.Event;

import java.util.List;
import java.util.Map;

public interface CompilationRepositoryCustom {
    List<Event> findEventsByIds(List<Long> eventIds);

    Map<Long, Long> countConfirmedRequests(List<Long> eventIds);

}
