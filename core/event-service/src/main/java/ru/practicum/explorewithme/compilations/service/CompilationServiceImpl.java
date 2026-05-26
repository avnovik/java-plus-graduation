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
import ru.practicum.explorewithme.category.client.CategoryClient;
import ru.practicum.explorewithme.category.dto.CategoryDto;
import ru.practicum.explorewithme.compilations.dto.CompilationDto;
import ru.practicum.explorewithme.compilations.dto.CompilationWithStats;
import ru.practicum.explorewithme.compilations.dto.NewCompilationDto;
import ru.practicum.explorewithme.compilations.dto.UpdateCompilationRequest;
import ru.practicum.explorewithme.compilations.mapper.CompilationMapper;
import ru.practicum.explorewithme.compilations.model.Compilation;
import ru.practicum.explorewithme.compilations.repository.CompilationRepository;
import ru.practicum.explorewithme.event.dto.EventStatsDto;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.event.repository.EventRepository;
import ru.practicum.explorewithme.request.client.RequestClient;
import ru.practicum.explorewithme.request.dto.EventConfirmedCountDto;
import ru.practicum.explorewithme.dto.error.NotFoundException;
import ru.practicum.explorewithme.user.client.UserClient;
import ru.practicum.explorewithme.user.dto.UserDto;
import ru.practicum.explorewithme.user.dto.UserShortDto;

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
    private final EventRepository eventRepository;
    private final RequestClient requestClient;
    private final CategoryClient categoryClient;
    private final UserClient userClient;


    @Override
    @Transactional
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {
        List<Long> eventIds = Optional.ofNullable(newCompilationDto.getEvents())
                .orElse(Collections.emptyList());

        if (eventIds.isEmpty()) {
            Compilation compilation = CompilationMapper
                    .toCompilation(newCompilationDto, Collections.emptyList());
            compilationRepository.save(compilation);
            return CompilationMapper.toDto(buildCompilationWithStats(compilation), this::loadCategory, this::loadInitiator);
        }

        if (eventIds.size() != new HashSet<>(eventIds).size()) {
            throw new IllegalArgumentException("Events must be unique");
        }

        List<Event> events = getEventsByEventsId(eventIds);
        Compilation compilation = CompilationMapper.toCompilation(newCompilationDto, events);
        Compilation saved = compilationRepository.save(compilation);

        return CompilationMapper.toDto(buildCompilationWithStats(saved), this::loadCategory, this::loadInitiator);
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
            compilationFromDb.setEvents(new ArrayList<>(events));
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

        return CompilationMapper.toDto(buildCompilationWithStats(updated), this::loadCategory, this::loadInitiator);
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

        return CompilationMapper.toDto(buildCompilationWithStats(compilationFromDb), this::loadCategory, this::loadInitiator);
    }

    @Override
    public List<CompilationDto> getCompilations(boolean pinned, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));
        List<Compilation> compilations = compilationRepository.findCompilationsByPinned(pinned, pageable);

        List<CompilationWithStats> param = buildCompilationWithStats(compilations);

        return CompilationMapper.toListDto(param, this::loadCategory, this::loadInitiator);
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
        Map<Long, Long> confirmedRequests = buildConfirmedRequests(eventIds);
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

    private Map<Long, Long> buildConfirmedRequests(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<EventConfirmedCountDto> counts = Optional.ofNullable(requestClient.getConfirmedCounts(eventIds))
                .orElse(Collections.emptyList());

        return counts.stream()
                .filter(c -> c.getEventId() != null)
                .collect(Collectors.toMap(
                        EventConfirmedCountDto::getEventId,
                        c -> c.getCnt() == null ? 0L : c.getCnt(),
                        Long::sum
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
        List<Event> events = eventRepository.findAllById(eventIds);

        if (events.size() != eventIds.size()) {
            Set<Long> foundIds = events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());

            List<Long> missing = eventIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            throw new IllegalArgumentException("Events not found: " + missing);
        }

        Map<Long, Event> byId = events.stream().collect(Collectors.toMap(Event::getId, e -> e));
        return eventIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private NotFoundException compilationNotFound(Long compilationId) {
        return new NotFoundException("Compilation with id=" + compilationId + " was not found");
    }

    private CategoryDto loadCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryClient.getCategory(categoryId);
    }

    private UserShortDto loadInitiator(Long userId) {
        if (userId == null) {
            return null;
        }
        UserDto user = userClient.getUser(userId);
        return new UserShortDto(user.getId(), user.getName());
    }

}
