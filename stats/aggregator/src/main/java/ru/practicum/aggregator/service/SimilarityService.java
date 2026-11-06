package ru.practicum.aggregator.service;

import ru.practicum.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.avro.UserActionAvro;

import java.util.List;

public interface SimilarityService {
    List<EventSimilarityAvro> process(UserActionAvro action);

    void putMinWeightSum(long eventA, long eventB, double sum);

    Double getMinWeightSum(long eventA, long eventB);
}
