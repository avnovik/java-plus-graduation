package ru.practicum.explorewithme.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.event.repository.EventRepository;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventCategoryController {

    private final EventRepository eventRepository;

    @GetMapping("/exists-by-category")
    public boolean existsByCategory(@RequestParam("categoryId") long categoryId) {
        return eventRepository.existsByCategoryId(categoryId);
    }
}
