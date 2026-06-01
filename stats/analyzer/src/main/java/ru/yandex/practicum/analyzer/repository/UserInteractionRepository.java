package ru.yandex.practicum.analyzer.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.analyzer.model.UserInteraction;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    Optional<UserInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserInteraction> findByUserId(Long userId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    @Query("""
            select interaction.eventId, sum(interaction.weight)
            from UserInteraction interaction
            where interaction.eventId in :eventIds
            group by interaction.eventId
            """)
    List<Object[]> sumWeightsByEventIds(Collection<Long> eventIds);
}
