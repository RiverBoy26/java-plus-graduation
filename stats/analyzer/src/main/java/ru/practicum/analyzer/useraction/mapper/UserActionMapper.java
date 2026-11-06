package ru.practicum.analyzer.useraction.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.analyzer.useraction.model.UserAction;
import ru.practicum.stats.avro.ActionTypeAvro;
import ru.practicum.stats.avro.UserActionAvro;

@UtilityClass
public class UserActionMapper {
    public UserAction mapToUserAction(UserActionAvro avro) {
        return UserAction.builder()
                .userId(avro.getUserId())
                .eventId(avro.getEventId())
                .rating(getWeight(avro.getActionType()))
                .timestamp(avro.getTimestamp())
                .build();
    }

    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}