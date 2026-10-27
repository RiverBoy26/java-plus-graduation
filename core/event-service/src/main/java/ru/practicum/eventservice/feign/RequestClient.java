package ru.practicum.eventservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", contextId = "event-requests", fallback = RequestClientFallback.class)
public interface RequestClient {
    @PostMapping("/internal/requests/confirmed-counts")
    Map<Long, Long> getConfirmedCounts(@RequestBody List<Long> eventIds);
}
