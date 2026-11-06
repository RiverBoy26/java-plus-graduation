package ru.practicum.collector.mapper;

import com.google.protobuf.util.Timestamps;
import lombok.experimental.UtilityClass;
import ru.practicum.stats.avro.ActionTypeAvro;
import ru.practicum.stats.avro.UserActionAvro;
import stats.service.collector.ActionTypeProto;
import stats.service.collector.UserActionProto;

import java.time.Instant;

@UtilityClass
public class UserActionMapper {
    public static UserActionAvro mapToUserActionAvro(UserActionProto proto) {
        return UserActionAvro.newBuilder()
                .setUserId(proto.getId())
                .setActionType(mapToActionTypeAvro(proto.getActionType()))
                .setEventId(proto.getEventId())
                .setTimestamp(Instant.ofEpochMilli(Timestamps.toMillis(proto.getTimestamp())))
                .build();
    }

    private static ActionTypeAvro mapToActionTypeAvro(ActionTypeProto proto) {
        return switch (proto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown action type" + proto);
        };
    }
}