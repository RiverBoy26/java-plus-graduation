package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.similarity.model.EventSimilarity;
import ru.practicum.analyzer.similarity.repository.SimilarityRepository;
import ru.practicum.analyzer.useraction.model.UserAction;
import ru.practicum.analyzer.useraction.repository.UserActionRepository;
import ru.practicum.ewm.stats.service.dashboard.RecommendedEventProto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {
    private final UserActionRepository userActionRepository;
    private final SimilarityRepository similarityRepository;
    private static final int K = 5;

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResult) {
        // события с которыми userId взаимодействовал недавно. вернуть пустой список, если их нет
        Set<UserAction> userInteractions = getRecentUserAction(userId, K);
        if (userInteractions.isEmpty()) return List.of();

        //Найти мероприятия, похожие на те,
        // что отобрали на предыдущем этапе, но при этом пользователь с ними не взаимодействовал.
        Map<Long, Double> similarEvents = findSimilarNewEvents(userId, userInteractions);
// Отсортировать найденные мероприятия по коэффициенту подобия от большего к меньшему.
// Выбрать из них первые N мероприятий.
        List<Long> topEvents = selectTopNNewEvents(similarEvents, K);

        List<RecommendedEventProto> recommendations = new ArrayList<>();

        for (Long eventId : topEvents) {
            //        Выгрузить K просмотренных мероприятий, максимально похожих на предсказываемое.
//        Важно найти именно максимально похожие мероприятия,
//        с которыми пользователь уже взаимодействовал.
            List<EventSimilarity> neighbours = findKNearestInteractedEvents(userId, eventId, K);

            if (neighbours.isEmpty()) {
                continue;
            }
            // Для всех мероприятий,
            // полученных на предыдущем этапе, выгрузить оценки, которые поставил пользователь.
            Map<Long, Double> ratings = getUserRatings(userId, neighbours, eventId);

            if (ratings.isEmpty()) {
                continue;
            }
            // Вычислить сумму взвешенных оценок.
            // Используя коэффициенты подобия, полученные на шаге a,
            // и оценки, полученные на шаге b, вычислить сумму взвешенных оценок
            // (перемножить оценки мероприятий с их коэффициентами подобия,
            // все полученные произведения сложить).
            double weightedRatingsSum =
                    calculateWeightedRatingsSum(
                            neighbours,
                            ratings,
                            eventId
                    );
            // Сложить все коэффициенты подобия, полученные на шаге a.
            double similaritySum =
                    calculateSimilaritySum(
                            neighbours,
                            ratings,
                            eventId
                    );

            if (similaritySum == 0.0) {
                continue;
            }
            // Поделить сумму взвешенных оценок на сумму коэффициентов.
            double predictedRating =
                    calculatePredictedRating(
                            weightedRatingsSum,
                            similaritySum
                    );

            recommendations.add(
                    RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(predictedRating)
                            .build()
            );
        }

        return recommendations;
    }

    // Поделить сумму взвешенных оценок на сумму коэффициентов.
    private double calculatePredictedRating(
            double weightedRatingsSum,
            double similaritySum
    ) {
        return weightedRatingsSum / similaritySum;
    }

    // Сложить все коэффициенты подобия, полученные в calculateWeightedRatingsSum.
    private double calculateSimilaritySum(
            List<EventSimilarity> neighbors,
            Map<Long, Double> ratings,
            long eventId
    ) {
        double sum = 0.0;

        for (EventSimilarity neighbor : neighbors) {

            long neighborEventId =
                    getOtherEventId(
                            neighbor,
                            eventId
                    );

            if (!ratings.containsKey(
                    neighborEventId
            )) {
                continue;
            }

            sum += neighbor.getScore();
        }

        return sum;
    }

    // Вычислить сумму взвешенных оценок.
// Используя коэффициенты подобия, полученные на шаге a,
// и оценки, полученные на шаге b, вычислить сумму взвешенных оценок
// (перемножить оценки мероприятий с их коэффициентами подобия,
// все полученные произведения сложить).
    private double calculateWeightedRatingsSum(
            List<EventSimilarity> neighbors,
            Map<Long, Double> ratings,
            long eventId
    ) {
        double sum = 0.0;

        for (EventSimilarity neighbor : neighbors) {

            long neighborEventId =
                    getOtherEventId(
                            neighbor,
                            eventId
                    );

            Double rating =
                    ratings.get(neighborEventId);

            if (rating == null) {
                continue;
            }

            sum += neighbor.getScore() * rating;
        }

        return sum;
    }

    // Для всех мероприятий,
// полученных на предыдущем этапе, выгрузить оценки, которые поставил пользователь.
    private Map<Long, Double> getUserRatings(
            long userId,
            List<EventSimilarity> neighbors,
            long eventId
    ) {
        List<Long> neighborEventIds = neighbors.stream()
                .map(similarity ->
                        getOtherEventId(similarity, eventId))
                .toList();

        return userActionRepository
                .findByUserIdAndEventIdIn(userId, neighborEventIds)
                .stream()
                .filter(action -> action.getRating() != null)
                .collect(Collectors.toMap(
                        UserAction::getEventId,
                        UserAction::getRating
                ));
    }

    //        Выгрузить K просмотренных мероприятий, максимально похожих на предсказываемое.
//        Важно найти именно максимально похожие мероприятия,
//        с которыми пользователь уже взаимодействовал.
    private List<EventSimilarity> findKNearestInteractedEvents(
            long userId,
            long eventId,
            int k
    ) {
        Set<Long> interactedEventIds =
                userActionRepository.findEventIdsByUserId(userId);

        return similarityRepository.findSimilarEvents(eventId)
                .stream()
                .filter(similarity -> {

                    long neighborEventId =
                            getOtherEventId(
                                    similarity,
                                    eventId
                            );

                    return interactedEventIds.contains(
                            neighborEventId
                    );
                })
                .sorted(
                        Comparator.comparing(
                                EventSimilarity::getScore
                        ).reversed()
                )
                .limit(k)
                .toList();
    }

    // Отсортировать найденные мероприятия по коэффициенту подобия от большего к меньшему.
// Выбрать из них первые N мероприятий.
    private List<Long> selectTopNNewEvents(
            Map<Long, Double> candidates,
            int n
    ) {
        return candidates.entrySet()
                .stream()
                .sorted(
                        Map.Entry.<Long, Double>comparingByValue()
                                .reversed()
                )
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }

    // события с которыми userId взаимодействовал недавно
    private Set<UserAction> getRecentUserAction(long userId, int n) {
        Instant now = Instant.now();
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
        Pageable pageable = PageRequest.of(0, n);
        return userActionRepository
                .findByUserIdAndTimestampBetweenOrderByTimestampDesc(userId, weekAgo, now, pageable)
                .stream().collect(Collectors.toSet());

    }

    //Найти мероприятия, похожие на те,
    // что отобрали на предыдущем этапе, но при этом пользователь с ними не взаимодействовал.
    private Map<Long, Double> findSimilarNewEvents(
            long userId,
            Set<UserAction> recentActions
    ) {
        Set<Long> interactedEventIds =
                userActionRepository.findEventIdsByUserId(userId);

        Map<Long, Double> candidates = new HashMap<>();

        for (UserAction action : recentActions) {

            List<EventSimilarity> similarities =
                    similarityRepository.findSimilarEvents(
                            action.getEventId()
                    );

            for (EventSimilarity similarity : similarities) {

                long similarEventId =
                        getOtherEventId(
                                similarity,
                                action.getEventId()
                        );

                if (interactedEventIds.contains(similarEventId)) {
                    continue;
                }

                candidates.merge(
                        similarEventId,
                        similarity.getScore(),
                        Math::max
                );
            }
        }

        return candidates;
    }

    private long getOtherEventId(EventSimilarity similarity, long eventId) {
        if (similarity.getEventA() == eventId) {
            return similarity.getEventB();
        }

        return similarity.getEventA();
    }

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResult) {
        List<EventSimilarity> unseenSimilarities = getSimilaritiesUserNotInteracted(eventId, userId, maxResult);

        List<RecommendedEventProto> result = new ArrayList<>();

        for (EventSimilarity similarity : unseenSimilarities) {
            Long id = (eventId == similarity.getEventA())
                    ? similarity.getEventB() : similarity.getEventA();
            RecommendedEventProto res = RecommendedEventProto.newBuilder()
                    .setEventId(id)
                    .setScore(similarity
                            .getScore()).build();
            result.add(res);
        }
        return result;
    }

    private List<EventSimilarity> getSimilaritiesUserNotInteracted(Long eventId, Long userId, int maxResult) {
        List<EventSimilarity> eventIds = similarityRepository.findSimilarEvents(eventId);

        Set<Long> eventUserInteracted = userActionRepository.findEventIdsByUserId(userId);

        List<EventSimilarity> unseenSimilarities = eventIds.stream().filter(similarity ->
                {
                    long similarEventId = eventId == similarity.getEventA()
                            ? similarity.getEventB()
                            : similarity.getEventA();

                    return !eventUserInteracted.contains(similarEventId);
                })
                .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                .limit(maxResult)
                .toList();
        return unseenSimilarities;
    }

    public List<RecommendedEventProto> getEventsRatings(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return List.of();

        return eventIds.stream().map(eventId -> RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(userActionRepository.sumRatingByEventId(eventId))
                .build()).toList();
    }
}