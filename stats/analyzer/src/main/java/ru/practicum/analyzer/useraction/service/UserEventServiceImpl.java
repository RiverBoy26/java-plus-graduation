package ru.practicum.analyzer.useraction.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.useraction.mapper.UserActionMapper;
import ru.practicum.analyzer.useraction.model.UserAction;
import ru.practicum.analyzer.useraction.repository.UserActionRepository;

import ru.practicum.stats.avro.UserActionAvro;
import ru.practicum.stats.avro.ActionTypeAvro;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserEventServiceImpl implements UserEventService {
    private final UserActionRepository repository;

    public UserAction saveUserAction(UserActionAvro avro) {
        Optional<UserAction> oldAction = repository.findByUserIdAndEventId(avro.getUserId(), avro.getEventId());

        if (oldAction.isEmpty()) {
            UserAction action = UserActionMapper.mapToUserAction(avro);
            return repository.save(action);
        }

        UserAction action = oldAction.get();
        double newWeight = getWeight(avro.getActionType());
        if (newWeight > oldAction.get().getRating()) {
            oldAction.get().setRating(getWeight(avro.getActionType()));
            return repository.save(oldAction.get());
        }
        return action;
    }

    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}