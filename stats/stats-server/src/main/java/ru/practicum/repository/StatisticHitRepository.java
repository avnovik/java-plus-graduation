package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.model.StatisticHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticHitRepository extends JpaRepository<StatisticHit, Long> {

    @Query("""
            select new ru.practicum.dto.ViewStatsDto(h.app, h.uri, count(h.id))
            from StatisticHit h
            where h.hitTime between :start and :end
            group by h.app, h.uri
            order by count(h.id) desc
            """)
    List<ViewStatsDto> getStats(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    @Query("""
            select new ru.practicum.dto.ViewStatsDto(h.app, h.uri, count(distinct h.ip))
            from StatisticHit h
            where h.hitTime between :start and :end
            group by h.app, h.uri
            order by count(distinct h.ip) desc
            """)
    List<ViewStatsDto> getUniqueStats(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("""
            select new ru.practicum.dto.ViewStatsDto(h.app, h.uri, count(h.id))
            from StatisticHit h
            where h.hitTime between :start and :end
              and h.uri in :uris
            group by h.app, h.uri
            order by count(h.id) desc
            """)
    List<ViewStatsDto> getStatsByUris(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("uris") List<String> uris);

    @Query("""
            select new ru.practicum.dto.ViewStatsDto(h.app, h.uri, count(distinct h.ip))
            from StatisticHit h
            where h.hitTime between :start and :end
              and h.uri in :uris
            group by h.app, h.uri
            order by count(distinct h.ip) desc
            """)
    List<ViewStatsDto> getUniqueStatsByUris(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("uris") List<String> uris);
}
