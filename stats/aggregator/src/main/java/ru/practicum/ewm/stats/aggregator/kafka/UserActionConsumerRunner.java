package ru.practicum.ewm.stats.aggregator.kafka;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.service.SimilarityService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumerRunner implements ApplicationRunner {

    private final KafkaConsumer<Long, UserActionAvro> consumer;
    private final Duration kafkaPollTimeout;
    private final SimilarityService similarityService;
    private final EventSimilaritySender eventSimilaritySender;

    @Override
    public void run(ApplicationArguments args) {
        log.debug("Starting user action consumer thread");
        Thread consumerThread = new Thread(this::pollUserActions, "user-action-consumer");
        consumerThread.setDaemon(false);
        consumerThread.start();
    }

    private void pollUserActions() {
        log.debug("User action consumer thread started");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(kafkaPollTimeout);
                if (!records.isEmpty()) {
                    log.debug("Polled {} user action records from Kafka", records.count());
                }
                records.forEach(record -> {
                    UserActionAvro action = record.value();
                    log.debug(
                            "Received user action. topic={}, partition={}, offset={}, eventId={}, userId={}, actionType={}",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            action.getEventId(),
                            action.getUserId(),
                            action.getActionType()
                    );
                    List<EventSimilarityAvro> similarities = similarityService.updateSimilarities(action);
                    log.debug("Calculated {} event similarities for eventId={}, userId={}, actionType={}",
                            similarities.size(),
                            action.getEventId(),
                            action.getUserId(),
                            action.getActionType());
                    similarities.forEach(similarity -> {
                        log.debug("Sending event similarity eventA={}, eventB={}, score={}, timestamp={}",
                                similarity.getEventA(),
                                similarity.getEventB(),
                                similarity.getScore(),
                                similarity.getTimestamp());
                        eventSimilaritySender.send(similarity);
                    });
                    eventSimilaritySender.flush();
                });
            } catch (Exception e) {
                log.error("Ошибка обработки действий пользователей в Aggregator", e);
            }
        }
    }
}
