package ru.yandex.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.grpc.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.grpc.RecommendedEventProto;
import ru.practicum.ewm.stats.grpc.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.grpc.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.grpc.UserPredictionsRequestProto;
import ru.yandex.practicum.analyzer.model.EventSimilarity;
import ru.yandex.practicum.analyzer.model.UserInteraction;
import ru.yandex.practicum.analyzer.repository.EventSimilarityRepository;
import ru.yandex.practicum.analyzer.repository.UserInteractionRepository;

/**
 * gRPC API Analyzer для получения рекомендаций и агрегированных взаимодействий.
 */
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final UserInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        Map<Long, Double> weightsByEvent = userInteractionRepository.sumWeightsByEventIds(request.getEventIdsList())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).doubleValue()
                ));

        request.getEventIdsList().forEach(eventId -> responseObserver.onNext(RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(weightsByEvent.getOrDefault(eventId, 0.0))
                .build()));
        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        Set<Long> interactedEventIds = userInteractionRepository.findByUserId(request.getUserId())
                .stream()
                .map(interaction -> interaction.getEventId())
                .collect(Collectors.toCollection(HashSet::new));

        List<EventSimilarity> similarities = eventSimilarityRepository.findByEventIdOrderByScoreDesc(request.getEventId());
        similarities.stream()
                .map(similarity -> toRecommendedSimilarEvent(request.getEventId(), similarity))
                .filter(recommendation -> !interactedEventIds.contains(recommendation.getEventId()))
                .limit(request.getMaxResults())
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        List<UserInteraction> interactions = userInteractionRepository.findByUserId(request.getUserId());
        Set<Long> interactedEventIds = interactions.stream()
                .map(UserInteraction::getEventId)
                .collect(Collectors.toCollection(HashSet::new));

        interactions.stream()
                .flatMap(interaction -> eventSimilarityRepository.findByEventIdOrderByScoreDesc(interaction.getEventId()).stream())
                .map(similarity -> getOtherEventId(similarity, interactedEventIds))
                .filter(candidateEventId -> !interactedEventIds.contains(candidateEventId))
                .distinct()
                .map(candidateEventId -> toPredictedRecommendation(candidateEventId, interactions))
                .filter(recommendation -> recommendation.getScore() > 0.0)
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .limit(request.getMaxResults())
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    private RecommendedEventProto toRecommendedSimilarEvent(long eventId, EventSimilarity similarity) {
        long similarEventId = getOtherEventId(similarity, eventId);
        return RecommendedEventProto.newBuilder()
                .setEventId(similarEventId)
                .setScore(similarity.getScore())
                .build();
    }

    private RecommendedEventProto toPredictedRecommendation(long candidateEventId, List<UserInteraction> interactions) {
        double weightedScoreSum = 0.0;
        double similaritySum = 0.0;

        for (UserInteraction interaction : interactions) {
            double similarity = eventSimilarityRepository.findByEventIds(candidateEventId, interaction.getEventId())
                    .map(EventSimilarity::getScore)
                    .orElse(0.0);
            weightedScoreSum += interaction.getWeight() * similarity;
            similaritySum += similarity;
        }

        double score = similaritySum == 0.0 ? 0.0 : weightedScoreSum / similaritySum;
        return RecommendedEventProto.newBuilder()
                .setEventId(candidateEventId)
                .setScore(score)
                .build();
    }

    private Long getOtherEventId(EventSimilarity similarity, Set<Long> interactedEventIds) {
        return interactedEventIds.contains(similarity.getEventA()) ? similarity.getEventB() : similarity.getEventA();
    }

    private Long getOtherEventId(EventSimilarity similarity, long eventId) {
        return similarity.getEventA().equals(eventId) ? similarity.getEventB() : similarity.getEventA();
    }
}
