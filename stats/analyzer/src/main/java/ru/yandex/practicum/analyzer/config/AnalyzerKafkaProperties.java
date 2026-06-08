package ru.yandex.practicum.analyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки Kafka для сервиса Analyzer.
 */
@ConfigurationProperties(prefix = "analyzer.kafka")
public record AnalyzerKafkaProperties(
        String bootstrapServers,
        String userActionsGroupId,
        String eventsSimilarityGroupId,
        Topics topics,
        long pollTimeoutMs
) {

    /**
     * Kafka топики, которые читает Analyzer.
     */
    public record Topics(
            String userActions,
            String eventsSimilarity
    ) {
    }
}
