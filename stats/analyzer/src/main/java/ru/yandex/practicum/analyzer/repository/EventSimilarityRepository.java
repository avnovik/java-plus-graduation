package ru.yandex.practicum.analyzer.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.analyzer.model.EventSimilarity;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("""
            select similarity
            from EventSimilarity similarity
            where similarity.eventA = :eventId or similarity.eventB = :eventId
            order by similarity.score desc
            """)
    List<EventSimilarity> findByEventIdOrderByScoreDesc(Long eventId);

    @Query("""
            select similarity
            from EventSimilarity similarity
            where (similarity.eventA = :eventA and similarity.eventB = :eventB)
               or (similarity.eventA = :eventB and similarity.eventB = :eventA)
            """)
    Optional<EventSimilarity> findByEventIds(Long eventA, Long eventB);
}
