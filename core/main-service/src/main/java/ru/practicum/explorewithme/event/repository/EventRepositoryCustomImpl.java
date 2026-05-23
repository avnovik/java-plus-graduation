package ru.practicum.explorewithme.event.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.JPAExpressions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.category.model.QCategory;
import ru.practicum.explorewithme.event.dto.EventAdminSettingSearchDto;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.event.model.EventState;
import ru.practicum.explorewithme.event.model.QEvent;
import ru.practicum.explorewithme.request.model.QRequest;
import ru.practicum.explorewithme.request.model.RequestStatus;
import ru.practicum.explorewithme.user.model.QUser;

import java.time.LocalDateTime;
import java.util.List;


@Repository
@RequiredArgsConstructor
public class EventRepositoryCustomImpl implements EventRepositoryCustom {

    private final JPAQueryFactory queryFactory;


    @Override
    public List<Event> findEventsToAdmin(EventAdminSettingSearchDto settingSearch) {

        QEvent event = QEvent.event;
        QUser user = QUser.user;
        QCategory category = QCategory.category;

        BooleanBuilder builder = new BooleanBuilder();

        if (settingSearch.getUsers() != null && !settingSearch.getUsers().isEmpty()) {
            builder.and(event.initiator.id.in(settingSearch.getUsers()));
        }

        if (settingSearch.getStates() != null && !settingSearch.getStates().isEmpty()) {
            builder.and(event.state.in(settingSearch.getStates()));
        }

        if (settingSearch.getCategories() != null && !settingSearch.getCategories().isEmpty()) {
            builder.and(event.category.id.in(settingSearch.getCategories()));
        }

        if (settingSearch.getRangeStart() != null) {
            builder.and(event.eventDate.goe(settingSearch.getRangeStart()));
        }

        if (settingSearch.getRangeEnd() != null) {
            builder.and(event.eventDate.loe(settingSearch.getRangeEnd()));
        }

        return queryFactory
                .selectFrom(event)
                .leftJoin(event.initiator, user).fetchJoin()
                .leftJoin(event.category, category).fetchJoin()
                .where(builder)
                .orderBy(event.eventDate.desc())
                .offset(settingSearch.getFrom())
                .limit(settingSearch.getSize())
                .fetch();
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

        QEvent event = QEvent.event;
        QUser user = QUser.user;
        QCategory category = QCategory.category;
        QRequest request = QRequest.request;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(event.state.eq(EventState.PUBLISHED));

        if (text != null && !text.isBlank()) {
            String q = text.toLowerCase();
            builder.and(event.annotation.lower().contains(q)
                    .or(event.description.lower().contains(q)));
        }

        if (categories != null && !categories.isEmpty()) {
            builder.and(event.category.id.in(categories));
        }

        if (paid != null) {
            builder.and(event.paid.eq(paid));
        }

        if (rangeStart == null && rangeEnd == null) {
            builder.and(event.eventDate.after(LocalDateTime.now()));
        } else {
            if (rangeStart != null) {
                builder.and(event.eventDate.goe(rangeStart));
            }
            if (rangeEnd != null) {
                builder.and(event.eventDate.loe(rangeEnd));
            }
        }

        if (Boolean.TRUE.equals(onlyAvailable)) {
            builder.and(
                    event.participantLimit.eq(0)
                            .or(
                                    JPAExpressions
                                            .select(request.count())
                                            .from(request)
                                            .where(request.event.id.eq(event.id)
                                                    .and(request.status.eq(RequestStatus.CONFIRMED)))
                                            .lt(event.participantLimit.longValue())
                            )
            );
        }

        var query = queryFactory
                .selectFrom(event)
                .leftJoin(event.initiator, user).fetchJoin()
                .leftJoin(event.category, category).fetchJoin()
                .where(builder);

        if (from != null && size != null) {
            query.offset(from).limit(size.longValue());
        }

        if (sortByEventDate) {
            query.orderBy(event.eventDate.desc());
        } else {
            query.orderBy(event.id.asc());
        }

        return query.fetch();
    }
}
