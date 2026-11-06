package ru.practicum.analyzer.useraction.service;

import ru.practicum.analyzer.useraction.model.UserAction;
import ru.practicum.stats.avro.UserActionAvro;

public interface UserEventService {
    UserAction saveUserAction(UserActionAvro avro);
}
