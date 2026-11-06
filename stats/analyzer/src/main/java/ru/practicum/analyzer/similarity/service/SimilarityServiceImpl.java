package ru.practicum.analyzer.similarity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.similarity.mapper.EventSimilarityMapper;
import ru.practicum.analyzer.similarity.model.EventSimilarity;
import ru.practicum.analyzer.similarity.repository.SimilarityRepository;
import ru.practicum.stats.avro.EventSimilarityAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityServiceImpl implements SimilarityService {
    private final SimilarityRepository repository;

    public EventSimilarity saveSimilarity(EventSimilarityAvro avro) {
        EventSimilarity similarity = EventSimilarityMapper.mapToEventSimilarity(avro);

        return repository.findByEventAAndEventB(
                        similarity.getEventA(),
                        similarity.getEventB()
                )
                .map(existing -> {
                    existing.setScore(similarity.getScore());
                    existing.setTimestamp(similarity.getTimestamp());
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(similarity));
    }
}