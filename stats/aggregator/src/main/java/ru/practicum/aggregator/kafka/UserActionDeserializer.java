package ru.practicum.aggregator.kafka;

import ru.practicum.stats.avro.UserActionAvro;

public class UserActionDeserializer extends AvroCommonDeserializer<UserActionAvro> {
    public UserActionDeserializer() {
        super(UserActionAvro.getClassSchema());
    }
}