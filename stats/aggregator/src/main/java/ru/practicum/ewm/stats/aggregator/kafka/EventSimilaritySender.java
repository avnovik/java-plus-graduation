package ru.practicum.ewm.stats.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilaritySender {

    private final KafkaProducer<Long, EventSimilarityAvro> producer;

    @Value("${aggregator.kafka.producer.topics.events-similarity}")
    private String eventsSimilarityTopic;

    public void send(EventSimilarityAvro similarity) {
        ProducerRecord<Long, EventSimilarityAvro> record = new ProducerRecord<>(
                eventsSimilarityTopic,
                similarity.getEventA(),
                similarity
        );
        try {
            var metadata = producer.send(record).get();
            producer.flush();
            log.info("Event similarity sent to topic {}, partition {}, offset {}, eventA={}, eventB={}, score={}, timestamp={}",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset(),
                    similarity.getEventA(),
                    similarity.getEventB(),
                    similarity.getScore(),
                    similarity.getTimestamp());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Отправка сходства мероприятий в Kafka была прервана", e);
        } catch (Exception e) {
            throw new KafkaException("Ошибка отправки сходства мероприятий в Kafka", e);
        }
    }

    public void flush() {
        producer.flush();
    }
}
