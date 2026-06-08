package ru.practicum.explorewithme.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.explorewithme.request.dto.EventConfirmedCountDto;

import java.util.List;

@FeignClient(name = "request-service", contextId = "requestClient", path = "/internal/requests")
public interface RequestClient {

    @GetMapping("/confirmed-count")
    List<EventConfirmedCountDto> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds);

    @GetMapping("/confirmed")
    Boolean hasConfirmedRequest(@RequestParam("userId") Long userId,
                                @RequestParam("eventId") Long eventId);
}
