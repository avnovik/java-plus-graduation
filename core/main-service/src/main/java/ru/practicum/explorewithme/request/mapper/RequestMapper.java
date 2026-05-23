package ru.practicum.explorewithme.request.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.request.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.request.model.Request;

import java.util.List;

@UtilityClass
public final class RequestMapper {

    public static ParticipationRequestDto toDto(Request request) {
        if (request == null) {
            return null;
        }

        return new ParticipationRequestDto(
                request.getId(),
                request.getCreated(),
                request.getEvent().getId(),
                request.getRequester().getId(),
                request.getStatus()
        );
    }

    public static List<ParticipationRequestDto> toDtoList(List<Request> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        return requests.stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    public static EventRequestStatusUpdateResult toEventRequestStatusUpdateResult(
            List<Request> confirmed,
            List<Request> rejected
    ) {
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(toDtoList(confirmed))
                .rejectedRequests(toDtoList(rejected))
                .build();
    }
}
