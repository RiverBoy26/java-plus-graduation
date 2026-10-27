package ru.practicum.comment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.common.dto.UserShortDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", contextId = "comment-user")
public interface UserClient {

    @GetMapping("/internal/users/{id}")
    UserShortDto getUser(@PathVariable Long id);

    @PostMapping("/internal/users/batch")
    Map<Long, UserShortDto> getUsers(@RequestBody List<Long> ids);
}