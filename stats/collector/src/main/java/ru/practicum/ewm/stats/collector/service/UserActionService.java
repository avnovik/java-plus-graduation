package ru.practicum.ewm.stats.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.collector.kafka.UserActionKafkaSender;
import ru.practicum.ewm.stats.collector.mapper.UserActionMapper;
import ru.practicum.ewm.stats.grpc.UserActionProto;

/**
 * Обрабатывает действия пользователей, полученные Collector.
 */
@Service
@RequiredArgsConstructor
public class UserActionService {

    private final UserActionMapper mapper;
    private final UserActionKafkaSender kafkaSender;

    /**
     * Отправляет действие пользователя в Kafka.
     */
    public void collect(UserActionProto action) {
        kafkaSender.send(mapper.toAvro(action));
    }
}
