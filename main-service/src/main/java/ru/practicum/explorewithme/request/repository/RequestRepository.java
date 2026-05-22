package ru.practicum.explorewithme.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.explorewithme.request.model.Request;
import ru.practicum.explorewithme.request.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    List<Request> findAllByRequesterId(Long requesterId);

    List<Request> findByRequesterId(Long userId);

    List<Request> findByEventId(Long eventId);

    List<Request> findByEventIdAndStatus(Long eventId, RequestStatus status);

    Optional<Request> findByRequesterIdAndEventId(Long userId, Long eventId);

    @Query("select r.event.id as eventId, count(r.id) as cnt " +
            "from Request r " +
            "where r.event.id in :eventIds and r.status = :status " +
            "group by r.event.id")
    List<EventConfirmedCount> countByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds,
                                                       @Param("status") RequestStatus status);

    interface EventConfirmedCount {
        Long getEventId();

        Long getCnt();
    }
}
