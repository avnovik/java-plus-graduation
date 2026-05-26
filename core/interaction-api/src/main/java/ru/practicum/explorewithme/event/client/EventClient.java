package ru.practicum.explorewithme.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.explorewithme.event.dto.EventForRequestDto;

@FeignClient(name = "event-service", contextId = "eventClient", path = "/internal/events")
public interface EventClient {

    @GetMapping("/{eventId}/for-request")
    EventForRequestDto getEventForRequest(@PathVariable("eventId") long eventId);
}
