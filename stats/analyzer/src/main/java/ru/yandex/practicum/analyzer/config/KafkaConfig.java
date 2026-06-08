package ru.yandex.practicum.analyzer.config;

import deserializer.EventSimilarityAvroDeserializer;
import deserializer.UserActionAvroDeserializer;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaConsumer<Long, UserActionAvro> userActionConsumer(AnalyzerKafkaProperties properties) {
        Properties consumerProperties = baseConsumerProperties(properties.bootstrapServers(), properties.userActionsGroupId());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class.getName());
        KafkaConsumer<Long, UserActionAvro> consumer = new KafkaConsumer<>(consumerProperties);
        consumer.subscribe(List.of(properties.topics().userActions()));
        return consumer;
    }

    @Bean
    public KafkaConsumer<Long, EventSimilarityAvro> eventSimilarityConsumer(AnalyzerKafkaProperties properties) {
        Properties consumerProperties = baseConsumerProperties(properties.bootstrapServers(), properties.eventsSimilarityGroupId());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSimilarityAvroDeserializer.class.getName());
        KafkaConsumer<Long, EventSimilarityAvro> consumer = new KafkaConsumer<>(consumerProperties);
        consumer.subscribe(List.of(properties.topics().eventsSimilarity()));
        return consumer;
    }

    @Bean
    public Duration kafkaPollTimeout(AnalyzerKafkaProperties properties) {
        return Duration.ofMillis(properties.pollTimeoutMs());
    }

    private Properties baseConsumerProperties(String bootstrapServers, String groupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        return properties;
    }
}
