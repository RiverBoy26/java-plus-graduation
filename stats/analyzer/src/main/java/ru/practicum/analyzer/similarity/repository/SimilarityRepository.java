
package ru.practicum.analyzer.similarity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.similarity.model.EventSimilarity;

import java.util.List;
import java.util.Optional;

public interface SimilarityRepository
        extends JpaRepository<EventSimilarity, Long> {

    @Query("""
            SELECT es
            FROM EventSimilarity es
            WHERE es.eventA = :eventId
               OR es.eventB = :eventId
            ORDER BY es.score DESC
            """)
    List<EventSimilarity> findSimilarEvents(
            @Param("eventId") long eventId
    );

    @Query("""
            SELECT es
            FROM EventSimilarity es
            WHERE es.eventA IN :eventIds
               OR es.eventB IN :eventIds
            ORDER BY es.score DESC
            """)
    List<EventSimilarity> findSimilarEventsByEventIds(
            @Param("eventIds") List<Long> eventIds
    );

    Optional<EventSimilarity> findByEventAAndEventB(
            Long eventA,
            Long eventB
    );
}
