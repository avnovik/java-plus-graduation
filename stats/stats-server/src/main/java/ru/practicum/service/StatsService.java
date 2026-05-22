package ru.practicum.service;

import ru.practicum.dto.EndPointHitDto;
import ru.practicum.dto.StatsParamDto;
import ru.practicum.dto.ViewStatsDto;

import java.util.Collection;

public interface StatsService {

    Collection<ViewStatsDto> getStats(StatsParamDto paramDto);

    void saveHit(EndPointHitDto hitDto);
}
