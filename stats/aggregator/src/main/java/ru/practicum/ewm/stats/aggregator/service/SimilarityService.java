package ru.practicum.ewm.stats.aggregator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
public class SimilarityService {

    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    public synchronized List<EventSimilarityAvro> updateSimilarities(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double newWeight = getWeight(action.getActionType());
        double oldWeight = getEventUserWeight(eventId, userId);

        if (newWeight <= oldWeight) {
            return List.of();
        }

        List<Long> otherEvents = eventUserWeights.keySet().stream()
                .filter(otherEventId -> otherEventId != eventId)
                .toList();

        eventUserWeights.computeIfAbsent(eventId, ignored -> new HashMap<>()).put(userId, newWeight);
        eventWeightSums.merge(eventId, newWeight - oldWeight, Double::sum);

        List<EventSimilarityAvro> similarities = new ArrayList<>();
        for (Long otherEventId : otherEvents) {
            double otherWeight = getEventUserWeight(otherEventId, userId);
            if (otherWeight == 0.0) {
                continue;
            }

            double oldMinWeight = Math.min(oldWeight, otherWeight);
            double newMinWeight = Math.min(newWeight, otherWeight);
            double minWeightDelta = newMinWeight - oldMinWeight;

            double minWeightsSum = getMinWeightsSum(eventId, otherEventId);
            if (minWeightDelta > 0.0) {
                minWeightsSum = addMinWeightsSum(eventId, otherEventId, minWeightDelta);
            }
            double similarity = roundScore(minWeightsSum / Math.sqrt(eventWeightSums.get(eventId) * eventWeightSums.get(otherEventId)));
            long eventA = Math.min(eventId, otherEventId);
            long eventB = Math.max(eventId, otherEventId);
            similarities.add(new EventSimilarityAvro(eventA, eventB, similarity, Instant.now()));
        }

        return similarities;
    }

    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    private double roundScore(double score) {
        return BigDecimal.valueOf(score)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double getEventUserWeight(long eventId, long userId) {
        return eventUserWeights.getOrDefault(eventId, Map.of()).getOrDefault(userId, 0.0);
    }

    private double addMinWeightsSum(long eventA, long eventB, double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        Map<Long, Double> eventSums = minWeightsSums.computeIfAbsent(first, ignored -> new HashMap<>());
        double updatedSum = eventSums.getOrDefault(second, 0.0) + delta;
        eventSums.put(second, updatedSum);
        return updatedSum;
    }

    private double getMinWeightsSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minWeightsSums.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
    }
}
