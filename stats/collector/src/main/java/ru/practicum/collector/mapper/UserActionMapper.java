
package ru.practicum.collector.mapper;

import com.google.protobuf.util.Timestamps;
import lombok.experimental.UtilityClass;
import ru.practicum.stats.avro.ActionTypeAvro;
import ru.practicum.stats.avro.UserActionAvro;
import stats.service.collector.ActionTypeProto;
import stats.service.collector.UserActionProto;

import java.time.Instant;

/**
 * Утилитарный класс для преобразования данных
 * о действиях пользователей из Protobuf в Avro.
 * Обеспечивает преобразование идентификаторов,
 * типов действий и временных меток.
 *
 * @see UserActionProto
 * @see UserActionAvro
 */
@UtilityClass
public class UserActionMapper {

    /**
     * Преобразование UserActionProto в UserActionAvro.
     * Переносит идентификаторы пользователя и мероприятия,
     * преобразует тип действия и временную метку.
     *
     * @param proto сообщение о действии пользователя в формате Protobuf
     * @return сообщение о действии пользователя в формате Avro
     * @throws IllegalArgumentException если тип действия неизвестен
     */
    public static UserActionAvro mapToUserActionAvro(
            UserActionProto proto
    ) {
        return UserActionAvro.newBuilder()
                .setUserId(proto.getId())
                .setActionType(
                        mapToActionTypeAvro(proto.getActionType())
                )
                .setEventId(proto.getEventId())
                .setTimestamp(
                        Instant.ofEpochMilli(
                                Timestamps.toMillis(proto.getTimestamp())
                        )
                )
                .build();
    }

    /**
     * Преобразование типа действия из Protobuf в Avro.
     * Поддерживает просмотр, регистрацию и лайк мероприятия.
     *
     * @param proto тип действия в формате Protobuf
     * @return соответствующий тип действия в формате Avro
     * @throws IllegalArgumentException если получен неизвестный тип действия
     */
    private static ActionTypeAvro mapToActionTypeAvro(
            ActionTypeProto proto
    ) {
        return switch (proto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException(
                    "Unknown action type" + proto
            );
        };
    }
}
