package ru.practicum.ewm.stats.aggregator.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import deserializer.UserActionAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import serialization.AvroBinarySerializer;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaConsumer<Long, UserActionAvro> userActionConsumer(
            @Value("${aggregator.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${aggregator.kafka.consumer.group-id}") String groupId,
            @Value("${aggregator.kafka.consumer.topics.user-actions}") String userActionsTopic
    ) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");

        KafkaConsumer<Long, UserActionAvro> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(List.of(userActionsTopic));
        return consumer;
    }

    @Bean
    public KafkaProducer<Long, EventSimilarityAvro> eventSimilarityProducer(
            @Value("${aggregator.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroBinarySerializer.class.getName());
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "false");
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaProducer<>(properties);
    }

    @Bean
    public Duration kafkaPollTimeout() {
        return Duration.ofMillis(500);
    }
}
