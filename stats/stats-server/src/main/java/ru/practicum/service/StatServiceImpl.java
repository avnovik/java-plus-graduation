package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.EndPointHitDto;
import ru.practicum.dto.StatsParamDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.mapper.StatisticHitMapper;
import ru.practicum.repository.StatisticHitRepository;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatsService {

    private final StatisticHitRepository statisticHitRepository;
    private final StatisticHitMapper statisticHitMapper;

    @Override
    public void saveHit(EndPointHitDto hitDto) {
        statisticHitRepository.save(statisticHitMapper.toEntity(hitDto));
    }

    @Override
    public Collection<ViewStatsDto> getStats(StatsParamDto paramDto) {
        if (paramDto.getStartTime() == null || paramDto.getEndTime() == null) {
            throw new IllegalArgumentException("start and end must not be null");
        }
        if (paramDto.getStartTime().isAfter(paramDto.getEndTime())) {
            throw new IllegalArgumentException("start must not be after end");
        }

        boolean unique = paramDto.isUniques();
        List<String> uris = paramDto.getUris();

        if (uris == null || uris.isEmpty()) {
            if (unique) {
                return statisticHitRepository.getUniqueStats(paramDto.getStartTime(), paramDto.getEndTime());
            }
            return statisticHitRepository.getStats(paramDto.getStartTime(), paramDto.getEndTime());
        }

        if (unique) {
            return statisticHitRepository.getUniqueStatsByUris(paramDto.getStartTime(), paramDto.getEndTime(), uris);
        }
        return statisticHitRepository.getStatsByUris(paramDto.getStartTime(), paramDto.getEndTime(), uris);
    }
}
