package ru.practicum.requestservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.common.dto.UserShortDto;

@FeignClient(name = "user-service", contextId = "request-user")
public interface UserClient {
    @GetMapping("/internal/users/{id}")
    UserShortDto getUser(@PathVariable("id") Long id);
}
