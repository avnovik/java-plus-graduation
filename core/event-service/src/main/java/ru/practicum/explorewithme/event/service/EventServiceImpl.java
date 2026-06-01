package ru.practicum.explorewithme.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.AnalyzerClient;
import ru.practicum.CollectorClient;
import ru.practicum.explorewithme.category.client.CategoryClient;
import ru.practicum.explorewithme.category.dto.CategoryDto;
import ru.practicum.explorewithme.common.paging.OffsetBasedPageRequest;
import ru.practicum.explorewithme.dto.error.ConditionsNotMetException;
import ru.practicum.explorewithme.dto.error.DependencyUnavailableException;
import ru.practicum.explorewithme.dto.error.NotFoundException;
import ru.practicum.explorewithme.dto.error.ValidationException;
import ru.practicum.explorewithme.event.dto.*;
import ru.practicum.explorewithme.event.mapper.EventMapper;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.event.model.EventState;
import ru.practicum.explorewithme.event.model.EventStateAction;
import ru.practicum.explorewithme.event.repository.EventRepository;
import ru.practicum.explorewithme.request.client.RequestClient;
import ru.practicum.explorewithme.request.dto.EventConfirmedCountDto;
import ru.practicum.explorewithme.user.client.UserClient;
import ru.practicum.explorewithme.user.dto.UserDto;
import ru.practicum.explorewithme.user.dto.UserShortDto;

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
    private final UserClient userClient;
    private final CategoryClient categoryClient;
    private final RequestClient requestClient;
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;

    @Override
    @Transactional
    public EventFullDto addEvent(long userId, NewEventDto dto) {
        if (dto.getEventDate() == null) {
            throw new IllegalArgumentException("eventDate must not be null");
        }
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Event date must be at least 2 hours in the future");
        }

        UserDto initiator = userClient.getUser(userId);
        long categoryId = dto.getCategory();
        CategoryDto category = categoryClient.getCategory(categoryId);

        Event event = EventMapper.toEvent(dto, initiator.getId(), category.getId());
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);

        Event saved = eventRepository.save(event);

        return EventMapper.toEventFullDto(saved, category, toUserShortDto(initiator), loadEventStats(saved));
    }

    @Override
    public List<EventShortDto> getEvents(long userId, int from, int size) {
        userClient.getUser(userId);

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

        return EventMapper.toEventShortDto(events, stats, this::loadCategory, this::loadInitiator);
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

        Integer repoFrom = sort == PublicEventSort.RATING ? null : from;
        Integer repoSize = sort == PublicEventSort.RATING ? null : size;

        List<Event> events = eventRepository.findPublicEvents(
                text,
                categories,
                paid,
                rangeStart,
                rangeEnd,
                false,
                repoFrom,
                repoSize,
                sortByEventDate
        );

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = filterOnlyAvailable(events);
        }

        Map<Long, EventStatsDto> stats = loadEventStats(events);
        List<EventShortDto> result = EventMapper.toEventShortDto(events, stats, this::loadCategory, this::loadInitiator);

        if (sort == PublicEventSort.RATING) {
            result.sort(Comparator.comparing(EventShortDto::getRating, Comparator.nullsFirst(Double::compareTo)).reversed());

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
        return EventMapper.toEventFullDto(event, loadCategory(event.getCategoryId()), loadInitiator(event.getInitiatorId()), stats);
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, int maxResults) {
        userClient.getUser(userId);

        List<Long> eventIds = analyzerClient.getRecommendationsForUser(userId, maxResults)
                .map(rec -> rec.getEventId())
                .toList();

        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Event> eventsById = eventRepository.findAllById(eventIds).stream()
                .filter(event -> event.getState() == EventState.PUBLISHED)
                .collect(Collectors.toMap(Event::getId, event -> event));

        List<Event> events = eventIds.stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, EventStatsDto> stats = loadEventStats(events);
        return EventMapper.toEventShortDto(events, stats, this::loadCategory, this::loadInitiator);
    }

    @Override
    public void likeEvent(Long userId, Long eventId) {
        userClient.getUser(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> eventNotFound(eventId));

        if (event.getState() != EventState.PUBLISHED) {
            throw eventNotFound(eventId);
        }

        Boolean visited = requestClient.hasConfirmedRequest(userId, eventId);
        if (!Boolean.TRUE.equals(visited)) {
            throw new ValidationException("Only confirmed participants can like events");
        }

        collectorClient.collectLike(userId, eventId);
    }

    @Override
    public List<EventFullDto> findAllEventsToAdmin(EventAdminSettingSearchDto settingSearchDto) {
        EventAdminSettingSearchDto dtoWithDefaultValue = addDefaultValueToSettingDto(settingSearchDto);
        List<Event> events = eventRepository.findEventsToAdmin(dtoWithDefaultValue);

        Map<Long, EventStatsDto> stats = loadEventStats(events);

        return EventMapper.toEventFullDto(events, stats, this::loadCategory, this::loadInitiator);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByIdToAdmin(Long eventId, UpdateEventAdminRequest adminRequest) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> eventNotFound(eventId));

        updateEventFields(event, adminRequest);
        applyAdminStateAction(event, adminRequest.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        EventStatsDto stats = loadEventStats(updatedEvent);

        return EventMapper.toEventFullDto(updatedEvent, loadCategory(updatedEvent.getCategoryId()), loadInitiator(updatedEvent.getInitiatorId()), stats);
    }

    @Override
    public EventFullDto getEventByIdToUser(Long userId, Long eventId) {

        userClient.getUser(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(() -> eventNotFound(eventId));
        EventStatsDto stats = loadEventStats(event);
        return EventMapper.toEventFullDto(event, loadCategory(event.getCategoryId()), loadInitiator(event.getInitiatorId()), stats);
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
        return EventMapper.toEventFullDto(updatedEvent, loadCategory(updatedEvent.getCategoryId()), loadInitiator(updatedEvent.getInitiatorId()), stats);
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
        Map<Long, Double> ratingByEventId = getRatingsByEventIds(eventIds);

        return eventIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> new EventStatsDto(
                                confirmedByEventId.getOrDefault(id, 0L),
                                ratingByEventId.getOrDefault(id, 0.0)
                        )
                ));
    }

    private EventStatsDto loadEventStats(Event event) {
        if (event == null) {
            return new EventStatsDto(0L, 0.0);
        }

        return loadEventStats(List.of(event))
                .getOrDefault(event.getId(), new EventStatsDto(0L, 0.0));
    }

    private Map<Long, Long> getConfirmedRequestsByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<EventConfirmedCountDto> rows;
        try {
            rows = requestClient.getConfirmedCounts(eventIds);
        } catch (Exception ex) {
            log.warn("Request-service call failed; confirmedRequests is unavailable. Message: {}", ex.getMessage());
            throw new DependencyUnavailableException("Request-service is unavailable");
        }

        Map<Long, Long> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (EventConfirmedCountDto row : rows) {
            if (row == null || row.getEventId() == null || row.getCnt() == null) {
                continue;
            }
            result.put(row.getEventId(), row.getCnt());
        }
        return result;
    }

    private List<Event> filterOnlyAvailable(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = getConfirmedRequestsByEventIds(ids);

        return events.stream()
                .filter(e -> e.getParticipantLimit() == 0
                        || confirmed.getOrDefault(e.getId(), 0L) < e.getParticipantLimit())
                .toList();
    }

    private Map<Long, Double> getRatingsByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return analyzerClient.getInteractionsCount(eventIds)
                    .collect(Collectors.toMap(
                            rec -> rec.getEventId(),
                            rec -> rec.getScore()
                    ));
        } catch (Exception ex) {
            log.warn("Analyzer request failed; returning rating=0. Message: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }


    private void updateEventFields(Event event, UpdateEventFields req) {

        if (req.getAnnotation() != null) {
            event.setAnnotation(req.getAnnotation());
        }
        if (req.getCategory() != null) {
            Long categoryId = req.getCategory();
            CategoryDto category = categoryClient.getCategory(categoryId);
            event.setCategoryId(category.getId());
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
        return toUserShortDto(user);
    }

    private UserShortDto toUserShortDto(UserDto user) {
        if (user == null) {
            return null;
        }
        return new UserShortDto(user.getId(), user.getName());
    }
}
