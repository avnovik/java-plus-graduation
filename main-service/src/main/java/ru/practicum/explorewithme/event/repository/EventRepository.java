package ru.practicum.explorewithme.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explorewithme.event.model.Event;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, EventRepositoryCustom {

    @EntityGraph(attributePaths = {"initiator", "category"})
    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"initiator", "category"})
    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);
}
