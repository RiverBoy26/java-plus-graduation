
package ru.practicum.analyzer.similarity.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.analyzer.similarity.model.EventSimilarity;
import ru.practicum.stats.avro.EventSimilarityAvro;

/**
 * Утилитарный класс для преобразования данных
 * о сходстве мероприятий из Avro в сущности базы данных.
 * Переносит идентификаторы мероприятий,
 * коэффициент сходства и временную метку расчёта.
 *
 * @see EventSimilarity
 * @see EventSimilarityAvro
 */
@UtilityClass
public class EventSimilarityMapper {

    /**
     * Преобразование EventSimilarityAvro
     * в сущность EventSimilarity.
     * Сохраняет идентификаторы пары мероприятий,
     * коэффициент сходства и временную метку.
     *
     * @param avro сообщение о сходстве мероприятий в формате Avro
     * @return сущность с информацией о сходстве двух мероприятий
     */
    public EventSimilarity mapToEventSimilarity(
            EventSimilarityAvro avro
    ) {
        return EventSimilarity.builder()
                .eventA(avro.getEventA())
                .eventB(avro.getEventB())
                .score(avro.getScore())
                .timestamp(avro.getTimestamp())
                .build();
    }
}