package ru.practicum.explorewithme.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explorewithme.event.model.Event;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, EventRepositoryCustom {

    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);
}
