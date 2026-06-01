package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.analyzer.model.EventSimilarity;
import ru.yandex.practicum.analyzer.repository.EventSimilarityRepository;

/**
 * Обновляет актуальные коэффициенты сходства мероприятий.
 */
@Service
@RequiredArgsConstructor
public class EventSimilarityService {

    private final EventSimilarityRepository repository;

    @Transactional
    public void save(EventSimilarityAvro similarity) {
        long eventA = Math.min(similarity.getEventA(), similarity.getEventB());
        long eventB = Math.max(similarity.getEventA(), similarity.getEventB());

        EventSimilarity eventSimilarity = repository.findByEventAAndEventB(eventA, eventB)
                .orElseGet(EventSimilarity::new);

        eventSimilarity.setEventA(eventA);
        eventSimilarity.setEventB(eventB);
        eventSimilarity.setScore(similarity.getScore());
        eventSimilarity.setTimestamp(similarity.getTimestamp());
        repository.save(eventSimilarity);
    }
}
