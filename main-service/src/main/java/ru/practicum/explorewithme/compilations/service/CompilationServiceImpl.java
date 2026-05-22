package ru.practicum.explorewithme.compilations.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.dto.StatsParamDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.explorewithme.compilations.dto.CompilationDto;
import ru.practicum.explorewithme.compilations.dto.CompilationWithStats;
import ru.practicum.explorewithme.compilations.dto.NewCompilationDto;
import ru.practicum.explorewithme.compilations.dto.UpdateCompilationRequest;
import ru.practicum.explorewithme.compilations.mapper.CompilationMapper;
import ru.practicum.explorewithme.compilations.model.Compilation;
import ru.practicum.explorewithme.compilations.repository.CompilationRepository;
import ru.practicum.explorewithme.event.dto.EventStatsDto;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final StatsClient statsClient;
    private final CompilationRepository compilationRepository;


    @Override
    @Transactional
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {
        List<Long> eventIds = Optional.ofNullable(newCompilationDto.getEvents())
                .orElse(Collections.emptyList());

        if (eventIds.isEmpty()) {
            Compilation compilation = CompilationMapper
                    .toCompilation(newCompilationDto, Collections.emptyList());
            compilationRepository.save(compilation);
            return CompilationMapper.toDto(buildCompilationWithStats(compilation));
        }

        if (eventIds.size() != new HashSet<>(eventIds).size()) {
            throw new IllegalArgumentException("Events must be unique");
        }

        List<Event> events = getEventsByEventsId(eventIds);
        Compilation compilation = CompilationMapper.toCompilation(newCompilationDto, events);
        Compilation saved = compilationRepository.save(compilation);

        return CompilationMapper.toDto(buildCompilationWithStats(saved));
    }


    @Transactional
    @Override
    public CompilationDto updateCompilation(UpdateCompilationRequest updateCompilationRequest, Long compilationId) {
        Compilation compilationFromDb = compilationRepository
                .findById(compilationId)
                .orElseThrow(() -> compilationNotFound(compilationId));
        List<Long> eventsUpdate = updateCompilationRequest.getEvents();
        if (eventsUpdate != null) {
            List<Event> events = getEventsByEventsId(eventsUpdate);
            compilationFromDb.setEvents(events);
        }
        Boolean pinned = updateCompilationRequest.getPinned();
        if (pinned != null) {
            compilationFromDb.setPinned(pinned);
        }
        String title = updateCompilationRequest.getTitle();
        if (title != null) {
            compilationFromDb.setTitle(title);
        }
        Compilation updated = compilationRepository.save(compilationFromDb);

        return CompilationMapper.toDto(buildCompilationWithStats(updated));
    }

    @Transactional
    @Override
    public void deleteCompilation(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> compilationNotFound(compId));
        compilationRepository.delete(compilation);
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilationFromDb = compilationRepository
                .findById(compId)
                .orElseThrow(() -> compilationNotFound(compId));

        return CompilationMapper.toDto(buildCompilationWithStats(compilationFromDb));
    }

    @Override
    public List<CompilationDto> getCompilations(boolean pinned, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));
        List<Compilation> compilations = compilationRepository.findCompilationsByPinned(pinned, pageable);

        List<CompilationWithStats> param = buildCompilationWithStats(compilations);

        return CompilationMapper.toListDto(param);
    }

    private List<CompilationWithStats> buildCompilationWithStats(List<Compilation> compilations) {
        if (compilations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Event> events = compilations
                .stream()
                .map(Compilation::getEvents)
                .flatMap(list -> list != null ? list.stream() : Stream.empty())
                .toList();

        Map<Long, EventStatsDto> statsDtoMap = buildStats(events);

        return compilations.stream()
                .map(compilation -> new CompilationWithStats(
                        compilation,
                        statsDtoMap
                ))
                .toList();
    }

    private CompilationWithStats buildCompilationWithStats(Compilation compilations) {

        List<Event> events = Optional.ofNullable(compilations.getEvents())
                .orElse(Collections.emptyList());
        Map<Long, EventStatsDto> statsDtoMap = buildStats(events);

        return new CompilationWithStats(compilations, statsDtoMap);
    }

    private Map<Long, EventStatsDto> buildStats(List<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .distinct()
                .toList();
        // key - id Event, val - confirmedRequest
        Map<Long, Long> confirmedRequests =
                Optional.ofNullable(compilationRepository.countConfirmedRequests(eventIds))
                        .orElse(Collections.emptyMap());
        // key - id Event, val - view
        Map<Long, Long> views = buildView(eventIds);

        return eventIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> new EventStatsDto(
                                confirmedRequests.getOrDefault(id, 0L),
                                views.getOrDefault(id, 0L)
                        )
                ));
    }


    private Map<Long, Long> buildView(List<Long> eventIds) {
        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        StatsParamDto statsParamDto = new StatsParamDto(LocalDateTime.now().minusYears(1),
                LocalDateTime.now(),
                uris,
                true);

        List<ViewStatsDto> stats = Optional.ofNullable(
                statsClient.getStats(statsParamDto)
        ).orElse(Collections.emptyList());

        return stats.stream()
                .map(stat -> {
                    String uri = stat.getUri();
                    if (uri == null) return null;

                    int idx = uri.lastIndexOf("/");
                    if (idx == -1 || idx == uri.length() - 1) return null;

                    try {
                        Long eventId = Long.parseLong(uri.substring(idx + 1));
                        return Map.entry(eventId, stat.getHits());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Long::sum
                ));
    }


    private List<Event> getEventsByEventsId(List<Long> eventIds) {

        List<Event> events = compilationRepository.findEventsByIds(eventIds);

        if (events.size() != eventIds.size()) {
            Set<Long> foundIds = events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());

            List<Long> missing = eventIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            throw new IllegalArgumentException("Events not found: " + missing);
        }
        return events;
    }

    private NotFoundException compilationNotFound(Long compilationId) {
        return new NotFoundException("Compilation with id=" + compilationId + " was not found");
    }

}
