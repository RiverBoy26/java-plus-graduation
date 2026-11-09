
package ru.practicum.analyzer.useraction.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.analyzer.useraction.model.UserAction;
import ru.practicum.stats.avro.ActionTypeAvro;
import ru.practicum.stats.avro.UserActionAvro;

/**
 * Утилитарный класс для преобразования данных
 * о действиях пользователей из Avro в сущности базы данных.
 * Рассчитывает вес взаимодействия пользователя
 * с мероприятием в зависимости от типа действия.
 *
 * @see UserAction
 * @see UserActionAvro
 */
@UtilityClass
public class UserActionMapper {

    /**
     * Преобразование UserActionAvro в сущность UserAction.
     * Переносит идентификаторы пользователя и мероприятия,
     * временную метку и вычисляет вес действия.
     *
     * @param avro сообщение о действии пользователя в формате Avro
     * @return сущность взаимодействия пользователя с мероприятием
     */
    public UserAction mapToUserAction(UserActionAvro avro) {
        return UserAction.builder()
                .userId(avro.getUserId())
                .eventId(avro.getEventId())
                .rating(getWeight(avro.getActionType()))
                .timestamp(avro.getTimestamp())
                .build();
    }

    /**
     * Получение весового коэффициента действия пользователя.
     * Используются следующие значения:
     * <ul>
     *     <li>VIEW — 0.4 (просмотр мероприятия)</li>
     *     <li>REGISTER — 0.8 (регистрация на мероприятие)</li>
     *     <li>LIKE — 1.0 (положительная оценка мероприятия)</li>
     * </ul>
     *
     * @param actionType тип действия пользователя
     * @return весовой коэффициент действия
     */
    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}
