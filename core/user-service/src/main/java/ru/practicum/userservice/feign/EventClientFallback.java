package ru.practicum.userservice.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.common.exception.EventServiceNotAvailableException;

@Component
@Slf4j
public class EventClientFallback implements FallbackFactory<EventClient> {

    @Override
    public EventClient create(Throwable cause) {
        return userId -> {
            log.info("Event client is not available for user service userId={}", userId, cause);
            throw new EventServiceNotAvailableException(userId, cause);
        };
    }
}