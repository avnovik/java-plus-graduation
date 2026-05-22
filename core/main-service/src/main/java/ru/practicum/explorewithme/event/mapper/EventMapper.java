package ru.practicum.explorewithme.event.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.category.dto.CategoryDto;
import ru.practicum.explorewithme.category.model.Category;
import ru.practicum.explorewithme.event.dto.*;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.user.dto.UserShortDto;
import ru.practicum.explorewithme.user.model.User;

import java.util.List;
import java.util.Map;

@UtilityClass
public class EventMapper {

    public static Event toEvent(NewEventDto dto, User initiator, Category category) {
        Event event = new Event();
        event.setAnnotation(dto.getAnnotation());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setTitle(dto.getTitle());

        event.setLat(dto.getLocation().getLat());
        event.setLon(dto.getLocation().getLon());

        event.setPaid(Boolean.TRUE.equals(dto.getPaid()));
        event.setParticipantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0);
        event.setRequestModeration(dto.getRequestModeration() == null || dto.getRequestModeration());

        event.setInitiator(initiator);
        event.setCategory(category);

        return event;
    }

    public static EventFullDto toEventFullDto(Event event, EventStatsDto statsDto) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .paid(event.isPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .title(event.getTitle())
                .state(event.getState())
                .category(toCategoryDto(event.getCategory()))
                .initiator(toUserShortDto(event.getInitiator()))
                .location(toLocationDto(event.getLat(), event.getLon()))
                .confirmedRequests(statsDto.getConfirmedRequests())
                .views(statsDto.getViews())
                .build();
    }

    public static List<EventFullDto> toEventFullDto(List<Event> events, Map<Long, EventStatsDto> statsDto) {
        return
                events.stream()
                        .map(event -> EventMapper.toEventFullDto(
                                event,
                                statsDto.getOrDefault(event.getId(), new EventStatsDto(0L, 0L))
                        ))
                        .toList();
    }

    private static CategoryDto toCategoryDto(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(category.getId(), category.getName());
    }

    private static UserShortDto toUserShortDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserShortDto(user.getId(), user.getName());
    }

    private static LocationDto toLocationDto(Float lat, Float lon) {
        if (lat == null || lon == null) {
            return null;
        }
        return new LocationDto(lat, lon);
    }

    public static EventShortDto toEventShortDto(Event event, EventStatsDto statsDto) {

        return new EventShortDto(
                event.getId(),
                event.getAnnotation(),
                toCategoryDto(event.getCategory()),
                statsDto.getConfirmedRequests(),
                event.getEventDate(),
                toUserShortDto(event.getInitiator()),
                event.isPaid(),
                event.getTitle(),
                statsDto.getViews()
        );
    }

    public static List<EventShortDto> toEventShortDto(List<Event> events, Map<Long, EventStatsDto> statsDto) {
        return
                events.stream()
                        .map(event -> EventMapper.toEventShortDto(
                                event,
                                statsDto.getOrDefault(event.getId(), new EventStatsDto(0L, 0L))
                        ))
                        .toList();
    }


}
