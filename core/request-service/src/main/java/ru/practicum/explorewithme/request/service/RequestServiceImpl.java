package ru.practicum.explorewithme.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.dto.error.ConflictException;
import ru.practicum.explorewithme.dto.error.NotFoundException;
import ru.practicum.explorewithme.event.client.EventClient;
import ru.practicum.explorewithme.event.dto.EventForRequestDto;
import ru.practicum.explorewithme.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.request.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.request.mapper.RequestMapper;
import ru.practicum.explorewithme.request.model.Request;
import ru.practicum.explorewithme.request.model.RequestStatus;
import ru.practicum.explorewithme.request.model.UpdateRequestStatus;
import ru.practicum.explorewithme.request.repository.RequestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final EventClient eventClient;

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        EventForRequestDto event = eventClient.getEventForRequest(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        return requestRepository.findByEventId(eventId)
                .stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        EventForRequestDto event = eventClient.getEventForRequest(eventId);

        // Нельзя участвовать в своём событии
        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Инициатор события не может отправить запрос на участие");
        }

        // Событие должно быть опубликовано
        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Событие не опубликовано");
        }

        // Нельзя отправить повторную заявку
        if (requestRepository.findByRequesterIdAndEventId(userId, eventId).isPresent()) {
            throw new ConflictException("Заявка уже существует");
        }

        // Проверка лимита участников
        if (event.getParticipantLimit() > 0) {
            long confirmedCount = requestRepository
                    .countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (confirmedCount >= event.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит участников");
            }
        }

        Request request = new Request();
        request.setRequesterId(userId);
        request.setEventId(eventId);

        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        // TODO: заменить на вызов user-service (internal API) и проверить что пользователь существует

        List<Request> requests = requestRepository.findAllByRequesterId(userId);

        return requests.stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("Пользователь не является владельцем запроса");
        }

        request.setStatus(RequestStatus.CANCELED);

        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequests(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest dto
    ) {
        EventForRequestDto event = eventClient.getEventForRequest(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        List<Request> requests = requestRepository.findAllById(dto.getRequestIds());

        for (Request request : requests) {
            if (!request.getEventId().equals(eventId)) {
                throw new ConflictException("Заявка не относится к данному событию");
            }
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Изменять можно только заявки со статусом PENDING");
            }
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        int limit = event.getParticipantLimit();

        if (dto.getStatus() == UpdateRequestStatus.CONFIRMED && limit > 0 && confirmedCount >= limit) {
            throw new ConflictException("У события достигнут лимит запросов на участие");
        }

        List<Request> confirmed = new ArrayList<>();
        List<Request> rejected = new ArrayList<>();

        for (Request request : requests) {
            if (dto.getStatus() == UpdateRequestStatus.CONFIRMED) {
                if (limit > 0 && confirmedCount >= limit) {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(request);
                } else {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(request);
                    confirmedCount++;
                }
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(request);
            }
        }

        if (dto.getStatus() == UpdateRequestStatus.CONFIRMED && limit > 0 && confirmedCount >= limit) {
            List<Request> pendingRequests = requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
            for (Request pending : pendingRequests) {
                pending.setStatus(RequestStatus.REJECTED);
            }
            requestRepository.saveAll(pendingRequests);
        }

        requestRepository.saveAll(requests);

        return RequestMapper.toEventRequestStatusUpdateResult(confirmed, rejected);
    }


}
