package ru.practicum.eventservice.feign;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RequestClientFallback implements RequestClient {

    @Override
    public Map<Long, Long> getConfirmedCounts(List<Long> eventIds) {
        return eventIds.stream().distinct().collect(Collectors.toMap(Function.identity(), id -> 0L));
    }
}
