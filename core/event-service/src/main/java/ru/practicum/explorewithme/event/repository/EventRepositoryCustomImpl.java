package ru.practicum.explorewithme.event.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.event.dto.EventAdminSettingSearchDto;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.event.model.EventState;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Repository
@RequiredArgsConstructor
public class EventRepositoryCustomImpl implements EventRepositoryCustom {

    private final EntityManager entityManager;


    @Override
    public List<Event> findEventsToAdmin(EventAdminSettingSearchDto settingSearch) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> event = cq.from(Event.class);

        List<Predicate> predicates = new ArrayList<>();

        if (settingSearch.getUsers() != null && !settingSearch.getUsers().isEmpty()) {
            predicates.add(event.get("initiatorId").in(settingSearch.getUsers()));
        }

        if (settingSearch.getStates() != null && !settingSearch.getStates().isEmpty()) {
            predicates.add(event.get("state").in(settingSearch.getStates()));
        }

        if (settingSearch.getCategories() != null && !settingSearch.getCategories().isEmpty()) {
            predicates.add(event.get("categoryId").in(settingSearch.getCategories()));
        }

        if (settingSearch.getRangeStart() != null) {
            predicates.add(cb.greaterThanOrEqualTo(event.get("eventDate"), settingSearch.getRangeStart()));
        }

        if (settingSearch.getRangeEnd() != null) {
            predicates.add(cb.lessThanOrEqualTo(event.get("eventDate"), settingSearch.getRangeEnd()));
        }

        cq.select(event)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(cb.desc(event.get("eventDate")));

        TypedQuery<Event> query = entityManager.createQuery(cq);
        query.setFirstResult(settingSearch.getFrom());
        query.setMaxResults(settingSearch.getSize());
        return query.getResultList();
    }

    @Override
    public List<Event> findPublicEvents(String text,
                                        List<Long> categories,
                                        Boolean paid,
                                        LocalDateTime rangeStart,
                                        LocalDateTime rangeEnd,
                                        Boolean onlyAvailable,
                                        Integer from,
                                        Integer size,
                                        boolean sortByEventDate) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> event = cq.from(Event.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(event.get("state"), EventState.PUBLISHED));

        if (text != null && !text.isBlank()) {
            String q = "%" + text.toLowerCase() + "%";
            predicates.add(
                    cb.or(
                            cb.like(cb.lower(event.get("annotation")), q),
                            cb.like(cb.lower(event.get("description")), q)
                    )
            );
        }

        if (categories != null && !categories.isEmpty()) {
            predicates.add(event.get("categoryId").in(categories));
        }

        if (paid != null) {
            predicates.add(cb.equal(event.get("paid"), paid));
        }

        if (rangeStart == null && rangeEnd == null) {
            predicates.add(cb.greaterThan(event.get("eventDate"), LocalDateTime.now()));
        } else {
            if (rangeStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(event.get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(event.get("eventDate"), rangeEnd));
            }
        }

        cq.select(event).where(predicates.toArray(Predicate[]::new));
        if (sortByEventDate) {
            cq.orderBy(cb.desc(event.get("eventDate")));
        } else {
            cq.orderBy(cb.asc(event.get("id")));
        }

        TypedQuery<Event> query = entityManager.createQuery(cq);
        if (from != null && size != null) {
            query.setFirstResult(from);
            query.setMaxResults(size);
        }
        return query.getResultList();
    }
}
