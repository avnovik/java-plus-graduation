package ru.practicum.explorewithme.event.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.category.dto.CategoryDto;
import ru.practicum.explorewithme.event.dto.EventForRequestDto;
import ru.practicum.explorewithme.event.dto.EventFullDto;
import ru.practicum.explorewithme.event.dto.EventShortDto;
import ru.practicum.explorewithme.event.dto.EventStatsDto;
import ru.practicum.explorewithme.event.dto.LocationDto;
import ru.practicum.explorewithme.event.dto.NewEventDto;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.user.dto.UserShortDto;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@UtilityClass
public class EventMapper {

    public static Event toEvent(NewEventDto dto, Long initiatorId, Long categoryId) {
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

        event.setInitiatorId(initiatorId);
        event.setCategoryId(categoryId);

        return event;
    }

    public static EventFullDto toEventFullDto(Event event,
                                             CategoryDto category,
                                             UserShortDto initiator,
                                             EventStatsDto statsDto) {
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
                .category(category)
                .initiator(initiator)
                .location(toLocationDto(event.getLat(), event.getLon()))
                .confirmedRequests(statsDto.getConfirmedRequests())
                .views(statsDto.getViews())
                .build();
    }

    public static List<EventFullDto> toEventFullDto(List<Event> events,
                                                   Map<Long, EventStatsDto> statsDto,
                                                   Function<Long, CategoryDto> categoryLoader,
                                                   Function<Long, UserShortDto> initiatorLoader) {
        return events.stream()
                .map(event -> toEventFullDto(
                        event,
                        categoryLoader.apply(event.getCategoryId()),
                        initiatorLoader.apply(event.getInitiatorId()),
                        statsDto.getOrDefault(event.getId(), new EventStatsDto(0L, 0L))
                ))
                .toList();
    }

    private static LocationDto toLocationDto(Float lat, Float lon) {
        if (lat == null || lon == null) {
            return null;
        }
        return new LocationDto(lat, lon);
    }

    public static EventShortDto toEventShortDto(Event event,
                                               EventStatsDto statsDto,
                                               CategoryDto category,
                                               UserShortDto initiator) {
        return new EventShortDto(
                event.getId(),
                event.getAnnotation(),
                category,
                statsDto.getConfirmedRequests(),
                event.getEventDate(),
                initiator,
                event.isPaid(),
                event.getTitle(),
                statsDto.getViews()
        );
    }

    public static List<EventShortDto> toEventShortDto(List<Event> events,
                                                     Map<Long, EventStatsDto> statsDto,
                                                     Function<Long, CategoryDto> categoryLoader,
                                                     Function<Long, UserShortDto> initiatorLoader) {
        return events.stream()
                .map(event -> toEventShortDto(
                        event,
                        statsDto.getOrDefault(event.getId(), new EventStatsDto(0L, 0L)),
                        categoryLoader.apply(event.getCategoryId()),
                        initiatorLoader.apply(event.getInitiatorId())
                ))
                .toList();
    }

    public static EventForRequestDto toEventForRequestDto(Event event) {
        if (event == null) {
            return null;
        }
        return new EventForRequestDto(
                event.getId(),
                event.getInitiatorId(),
                Boolean.TRUE.equals(event.getRequestModeration()),
                event.getParticipantLimit(),
                event.getState() == null ? null : event.getState().name()
        );
    }
}
