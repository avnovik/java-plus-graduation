package ru.practicum.explorewithme.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.request.dto.EventConfirmedCountDto;
import ru.practicum.explorewithme.request.model.RequestStatus;
import ru.practicum.explorewithme.request.repository.RequestRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalRequestService {

    private final RequestRepository requestRepository;

    public List<EventConfirmedCountDto> getConfirmedCounts(List<Long> eventIds) {
        return requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED)
                .stream()
                .map(r -> new EventConfirmedCountDto(r.getEventId(), r.getCnt()))
                .toList();
    }
}
