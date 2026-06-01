package ru.practicum.ewm.stats.collector.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionKafkaSender {

    private final KafkaProducer<Long, UserActionAvro> producer;

    @Value("${collector.kafka.topics.user-actions}")
    private String userActionsTopic;

    /**
     * Отправляет действие пользователя в топик пользовательских действий.
     */
    public void send(UserActionAvro action) {
        ProducerRecord<Long, UserActionAvro> record = new ProducerRecord<>(
                userActionsTopic,
                action.getEventId(),
                action
        );
        try {
            var metadata = producer.send(record).get();
            log.info("User action sent to topic {}, partition {}, offset {}",
                    metadata.topic(), metadata.partition(), metadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Отправка действия пользователя в Kafka была прервана", e);
        } catch (Exception e) {
            throw new KafkaException("Ошибка отправки действия пользователя в Kafka", e);
        }
    }
}
