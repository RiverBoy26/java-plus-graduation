package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.stats.avro.ActionTypeAvro;
import ru.practicum.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.avro.UserActionAvro;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityService {
    //хранит веса пользователей, взаимодействовавших с меропрятием(и)
    private final Map<Long, Map<Long, Double>> userActionWeights;
    // хранит сумму весов мероприятия
    private final Map<Long, Double> eventWeightsSums;
    // хранит сумму минимальных весов, для пар мероприятий
    private final Map<Long, Map<Long, Double>> minWeightsSums;

    public List<EventSimilarityAvro> process(UserActionAvro action) {
        Long eventA = action.getEventId();
        Map<Long, Double> users = userActionWeights.get(eventA);

        double oldWeight = users == null ? 0.0 : users.getOrDefault(action.getUserId(), 0.0);

        double newWeight = getWeight(action.getActionType());
        double difference = newWeight - oldWeight;

        if (newWeight <= oldWeight) {
            return List.of();
        }

        Set<Long> changedEvents = saveMinWeightsSums(action, oldWeight, newWeight);
        saveNewWeight(action);
        saveEventWeightSum(action, difference);

        Collection<Long> eventsToRecalculate;

        if (users == null) {
            eventsToRecalculate = new ArrayList<>(userActionWeights.keySet());
        } else {
            eventsToRecalculate = changedEvents;
        }

        List<EventSimilarityAvro> result = new ArrayList<>();
        for (Long eventB : eventsToRecalculate) {
            if (eventA.equals(eventB)) {
                continue;
            }
            double score = countSimilarity(eventA, eventB);

            if (score == 0.0) {
                continue;
            }
            long first = Math.min(eventA, eventB);
            long second = Math.max(eventA, eventB);
            log.info(
                    "Calculated similarity: eventA={}, eventB={}, score={}",
                    eventA,
                    eventB,
                    score
            );
            result.add(EventSimilarityAvro.newBuilder()
                    .setEventA(first)
                    .setEventB(second)
                    .setScore(score)
                    .setTimestamp(action.getTimestamp())
                    .build());
        }
        log.info(
                "Generated {} similarity events for source event={}",
                result.size(),
                eventA
        );
        return result;
    }

    // рассчитывает сходство двух мероприятий
    private double countSimilarity(Long eventA, Long eventB) {
        double numerator = getMinWeightSum(eventA, eventB);
        if (numerator == 0.0) return 0.0;
        double denominator = eventWeightsSums.get(eventA) * eventWeightsSums.get(eventB);
        if (denominator == 0.0) return 0.0;
        return numerator / Math.sqrt(denominator);
    }

    // считает сумму минимальных весов всех пользователей,
    // взаимодействовавших с обоими сравниваемыми мероприятиями
    private Set<Long> saveMinWeightsSums(UserActionAvro action, double oldWeightA, double newWeightA) {
        Set<Long> changedEvents = new HashSet<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userActionWeights.entrySet()) {
            Long eventB = entry.getKey();

            if (Objects.equals(action.getEventId(), eventB)) {
                continue;
            }
            Double weightB = entry.getValue().get(action.getUserId());

            if (weightB == null) {
                continue;
            }

            changedEvents.add(eventB);

            double olnMin = Math.min(oldWeightA, weightB);
            double newMin = Math.min(newWeightA, weightB);

            double difference = newMin - olnMin;

            if (difference > 0) {
                double oldSum = getMinWeightSum(action.getEventId(), eventB);
                putMinWeightSum(action.getEventId(), eventB, oldSum + difference);
                changedEvents.add(eventB);
            }
        }
        return changedEvents;
    }

    public void putMinWeightSum(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    public Double getMinWeightSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        Map<Long, Double> inner = minWeightsSums.get(first);
        return inner == null ? 0.0 : inner.getOrDefault(second, 0.0);
    }


    // считает и сохраняет сумму весов для каждого события (событие посчитано, добавляется разница)
    private void saveEventWeightSum(UserActionAvro action, double difference) {
        eventWeightsSums.merge(
                action.getEventId(),
                difference,
                Double::sum
        );

    }

    // считает и сохраняет новые веса в userActionWeights
    private void saveNewWeight(UserActionAvro userAction) {
        userActionWeights
                .computeIfAbsent(userAction.getEventId(), e -> new HashMap<>())
                .put(
                        userAction.getUserId(),
                        getWeight(userAction.getActionType())
                );
    }


    // преобразует ActionType в веса
    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

}