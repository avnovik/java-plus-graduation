package ru.practicum.explorewithme.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import ru.practicum.StatsClient;
import ru.practicum.dto.StatsParamDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.explorewithme.category.model.Category;
import ru.practicum.explorewithme.category.repository.CategoryRepository;
import ru.practicum.explorewithme.common.paging.OffsetBasedPageRequest;
import ru.practicum.explorewithme.event.dto.*;
import ru.practicum.explorewithme.event.mapper.EventMapper;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.event.model.EventState;
import ru.practicum.explorewithme.event.model.EventStateAction;
import ru.practicum.explorewithme.event.repository.EventRepository;
import ru.practicum.explorewithme.exceptions.ConditionsNotMetException;
import ru.practicum.explorewithme.exceptions.NotFoundException;
import ru.practicum.explorewithme.exceptions.ValidationException;
import ru.practicum.explorewithme.request.model.RequestStatus;
import ru.practicum.explorewithme.request.repository.RequestRepository;
import ru.practicum.explorewithme.user.model.User;
import ru.practicum.explorewithme.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final int MIN_HOURS_BEFORE_EVENT = 2; // openApi 1353
    private static final LocalDateTime STATS_START = LocalDateTime.of(1999, 1, 1, 0, 0);

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventFullDto addEvent(long userId, NewEventDto dto) {
        if (dto.getEventDate() == null) {
            throw new IllegalArgumentException("eventDate must not be null");
        }
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Event date must be at least 2 hours in the future");
        }

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> userNotFound(userId));

        long categoryId = dto.getCategory();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> categoryNotFound(categoryId));

        Event event = EventMapper.toEvent(dto, initiator, category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);

        Event saved = eventRepository.save(event);

        return EventMapper.toEventFullDto(saved, loadEventStats(saved));
    }

    @Override
    public List<EventShortDto> getEvents(long userId, int from, int size) {
        userRepository.findById(userId)
                .orElseThrow(() -> userNotFound(userId));

        Pageable pageable = new OffsetBasedPageRequest(from, size, Sort.by(Sort.Direction.ASC, "id"));
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable).getContent();

        if (events.isEmpty()) {
            final int maxAttempts = 20;
            log.info("Events list is empty for userId={} (from={}, size={}); retrying up to {} times", userId, from, size,
                    maxAttempts);
            for (int attempt = 1; attempt <= maxAttempts && events.isEmpty(); attempt++) {
                log.debug("Events list is empty for userId={}, retry attempt {}/{}", userId, attempt, maxAttempts);
                try {
                    long backoffMs = Math.min(200L * attempt, 1000L);
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                events = eventRepository.findAllByInitiatorId(userId, pageable).getContent();
            }
        }

        if (events.isEmpty()) {
            log.info("No events found for userId={} after retries (from={}, size={})", userId, from, size);
            return Collections.emptyList();
        }


        Map<Long, EventStatsDto> stats = loadEventStats(events);

        return EventMapper.toEventShortDto(events, stats);
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text,
                                               List<Long> categories,
                                               Boolean paid,
                                               LocalDateTime rangeStart,
                                               LocalDateTime rangeEnd,
                                               Boolean onlyAvailable,
                                               PublicEventSort sort,
                                               int from,
                                               int size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("rangeStart must not be after rangeEnd");
        }
        boolean sortByEventDate = sort == null || sort == PublicEventSort.EVENT_DATE;

        Integer repoFrom = sort == PublicEventSort.VIEWS ? null : from;
        Integer repoSize = sort == PublicEventSort.VIEWS ? null : size;

        List<Event> events = eventRepository.findPublicEvents(
                text,
                categories,
                paid,
                rangeStart,
                rangeEnd,
                onlyAvailable,
                repoFrom,
                repoSize,
                sortByEventDate
        );

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, EventStatsDto> stats = loadEventStats(events);
        List<EventShortDto> result = EventMapper.toEventShortDto(events, stats);

        if (sort == PublicEventSort.VIEWS) {
            result.sort(Comparator.comparing(EventShortDto::getViews, Comparator.nullsFirst(Long::compareTo)).reversed());

            int start = Math.min(from, result.size());
            int end = Math.min(from + size, result.size());
            return result.subList(start, end);
        }

        return result;
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> eventNotFound(eventId));

        if (event.getState() != EventState.PUBLISHED) {
            throw eventNotFound(eventId);
        }

        EventStatsDto stats = loadEventStats(event);
        return EventMapper.toEventFullDto(event, stats);
    }

    @Override
    public List<EventFullDto> findAllEventsToAdmin(EventAdminSettingSearchDto settingSearchDto) {
        EventAdminSettingSearchDto dtoWithDefaultValue = addDefaultValueToSettingDto(settingSearchDto);
        List<Event> events = eventRepository.findEventsToAdmin(dtoWithDefaultValue);

        Map<Long, EventStatsDto> stats = loadEventStats(events);

        return EventMapper.toEventFullDto(events, stats);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByIdToAdmin(Long eventId, UpdateEventAdminRequest adminRequest) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> eventNotFound(eventId));

        updateEventFields(event, adminRequest);
        applyAdminStateAction(event, adminRequest.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        EventStatsDto stats = loadEventStats(updatedEvent);

        return EventMapper.toEventFullDto(updatedEvent, stats);
    }

    @Override
    public EventFullDto getEventByIdToUser(Long userId, Long eventId) {

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(() -> eventNotFound(eventId));
        EventStatsDto stats = loadEventStats(event);
        return EventMapper.toEventFullDto(event, stats);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByIdToUser(Long userId, Long eventId, UpdateEventUserRequest requestDto) {
        if (requestDto.getStateAction() != null && (requestDto.getStateAction() == EventStateAction.PUBLISH_EVENT ||
                requestDto.getStateAction() == EventStateAction.REJECT_EVENT)) {
            throw new ConditionsNotMetException("User cannot perform this state action");
        }
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(() -> eventNotFound(eventId));
        validationUpdateEventUserRequest(event, requestDto);
        updateEventFields(event, requestDto);
        applyUserStateAction(event, requestDto.getStateAction());
        Event updatedEvent = eventRepository.save(event);
        EventStatsDto stats = loadEventStats(updatedEvent);
        return EventMapper.toEventFullDto(updatedEvent, stats);
    }


    private EventAdminSettingSearchDto addDefaultValueToSettingDto(EventAdminSettingSearchDto settingSearchDto) {
        return new EventAdminSettingSearchDto(
                settingSearchDto.getUsers(),
                settingSearchDto.getStates(),
                settingSearchDto.getCategories(),
                settingSearchDto.getRangeStart(),
                settingSearchDto.getRangeEnd(),
                settingSearchDto.getFrom() != null ? settingSearchDto.getFrom() : 0,
                settingSearchDto.getSize() != null ? settingSearchDto.getSize() : 10
        );
    }

    private Map<Long, EventStatsDto> loadEventStats(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmedByEventId = getConfirmedRequestsByEventIds(eventIds);
        Map<Long, Long> viewsByEventId = getViewsByEventIds(eventIds);

        return eventIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> new EventStatsDto(
                                confirmedByEventId.getOrDefault(id, 0L),
                                viewsByEventId.getOrDefault(id, 0L)
                        )
                ));
    }

    private EventStatsDto loadEventStats(Event event) {
        if (event == null) {
            return new EventStatsDto(0L, 0L);
        }

        return loadEventStats(List.of(event))
                .getOrDefault(event.getId(), new EventStatsDto(0L, 0L));
    }

    private Map<Long, Long> getConfirmedRequestsByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<RequestRepository.EventConfirmedCount> rows =
                requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED);

        Map<Long, Long> result = new HashMap<>();
        for (RequestRepository.EventConfirmedCount row : rows) {
            result.put(row.getEventId(), row.getCnt());
        }
        return result;
    }

    private Map<Long, Long> getViewsByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        StatsParamDto params = new StatsParamDto(STATS_START, LocalDateTime.now(), uris, true);

        List<ViewStatsDto> stats;
        try {
            stats = statsClient.getStats(params);
            log.debug("Stats response: size={}", stats == null ? 0 : stats.size());
        } catch (RestClientException ex) {
            log.warn("Stats request failed; returning views=0. Message: {}", ex.getMessage());
            return Collections.emptyMap();
        }
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> result = new HashMap<>();
        for (ViewStatsDto row : stats) {
            if (row == null || row.getUri() == null || row.getHits() == null) {
                continue;
            }
            Long eventId = extractEventId(row.getUri());
            if (eventId == null) {
                continue;
            }
            result.merge(eventId, row.getHits(), Long::sum);
        }
        return result;
    }

    private Long extractEventId(String uri) {
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == uri.length() - 1) {
            return null;
        }
        String idPart = uri.substring(lastSlash + 1);
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private void updateEventFields(Event event, UpdateEventFields req) {

        if (req.getAnnotation() != null) {
            event.setAnnotation(req.getAnnotation());
        }
        if (req.getCategory() != null) {
            Long categoryId = req.getCategory();
            event.setCategory(categoryRepository.findById(categoryId)
                    .orElseThrow(() -> categoryNotFound(categoryId)));
        }

        if (req.getDescription() != null) {
            event.setDescription(req.getDescription());
        }

        if (req.getEventDate() != null) {
            event.setEventDate(req.getEventDate());
        }

        if (req.getLocation() != null) {
            event.setLat(req.getLocation().getLat());
            event.setLon(req.getLocation().getLon());
        }

        if (req.getPaid() != null) {
            event.setPaid(req.getPaid());
        }

        if (req.getParticipantLimit() != null) {
            event.setParticipantLimit(req.getParticipantLimit());
        }

        if (req.getRequestModeration() != null) {
            event.setRequestModeration(req.getRequestModeration());
        }

        if (req.getTitle() != null) {
            event.setTitle(req.getTitle());
        }
    }

    private void applyAdminStateAction(Event event, EventStateAction action) {
        if (action == null) {
            return;
        }

        switch (action) {
            case PUBLISH_EVENT -> publishEvent(event);
            case REJECT_EVENT -> rejectEvent(event);
            case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
            case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
        }
    }

    private void publishEvent(Event event) {
        if (event.getState() != EventState.PENDING) {
            throw new ConditionsNotMetException(
                    "Cannot publish event in state: " + event.getState()
            );
        }

        LocalDateTime publishTime = LocalDateTime.now();

        if (event.getEventDate().isBefore(publishTime.plusHours(1))) {
            throw new ConditionsNotMetException(
                    "Event date must be at 1 hour after publish time"
            );
        }

        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(publishTime);
    }

    private void rejectEvent(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConditionsNotMetException(
                    "Cannot reject a published event"
            );
        }

        event.setState(EventState.CANCELED);
    }

    private void validationUpdateEventUserRequest(Event event, UpdateEventUserRequest requestDto) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConditionsNotMetException("Cannot update a published event");
        }
        if (requestDto.getEventDate() != null) {
            if (requestDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Event date must be at least 2 hours from now");
            }
        }
    }

    private void applyUserStateAction(Event event, EventStateAction stateAction) {
        if (stateAction == null) {
            return;
        }
        switch (stateAction) {
            case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
            case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
        }
    }


    private NotFoundException eventNotFound(Long eventId) {
        return new NotFoundException("Event with id=" + eventId + " was not found");
    }

    private NotFoundException categoryNotFound(Long categoryId) {
        return new NotFoundException("Category with id=" + categoryId + " was not found");
    }

    private NotFoundException userNotFound(Long userId) {
        return new NotFoundException("User with id=" + userId + " was not found");
    }
}
