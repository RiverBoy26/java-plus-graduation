package ru.practicum.analyzer.similarity.service;

import ru.practicum.analyzer.similarity.model.EventSimilarity;
import ru.practicum.stats.avro.EventSimilarityAvro;

public interface SimilarityService {
    EventSimilarity saveSimilarity(EventSimilarityAvro avro);
}
