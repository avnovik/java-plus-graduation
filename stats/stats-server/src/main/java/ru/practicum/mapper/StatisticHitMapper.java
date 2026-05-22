package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.EndPointHitDto;
import ru.practicum.model.StatisticHit;

@Component
public class StatisticHitMapper {

    public StatisticHit toEntity(EndPointHitDto dto) {
        StatisticHit hit = new StatisticHit();
        hit.setApp(dto.getApp());
        hit.setUri(dto.getUri());
        hit.setIp(dto.getIp());
        hit.setHitTime(dto.getTimestamp());
        return hit;
    }
}
