package ru.practicum.explorewithme.compilations.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.event.model.QEvent;
import ru.practicum.explorewithme.request.model.QRequest;
import ru.practicum.explorewithme.request.model.RequestStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CompilationRepositoryCustomImpl implements CompilationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Event> findEventsByIds(List<Long> eventIds) {
        QEvent event = QEvent.event;
        return queryFactory
                .selectFrom(event)
                .leftJoin(event.category).fetchJoin()
                .leftJoin(event.initiator).fetchJoin()
                .where(event.id.in(eventIds))
                .fetch();
    }

    @Override
    public Map<Long, Long> countConfirmedRequests(List<Long> eventIds) {
        QRequest request = QRequest.request;
        return queryFactory
                .select(request.event.id, request.count())
                .from(request)
                .where(request.event.id.in(eventIds),
                        request.status.eq(RequestStatus.CONFIRMED))
                .groupBy(request.event.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(t -> t.get(0, Long.class),
                        t -> t.get(1, Long.class)));

    }
}
