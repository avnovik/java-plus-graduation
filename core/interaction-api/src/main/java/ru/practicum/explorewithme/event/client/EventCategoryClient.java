package ru.practicum.explorewithme.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "event-service", contextId = "eventCategoryClient", path = "/internal/events")
public interface EventCategoryClient {

    @GetMapping("/exists-by-category")
    boolean existsByCategory(@RequestParam("categoryId") long categoryId);
}
