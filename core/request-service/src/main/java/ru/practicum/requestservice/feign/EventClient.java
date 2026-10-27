package ru.practicum.requestservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.common.dto.EventInfoDto;

@FeignClient(name = "event-service", contextId = "request-event")
public interface EventClient {
    @GetMapping("/internal/events/{id}")
    EventInfoDto getEvent(@PathVariable("id") Long id);
}
