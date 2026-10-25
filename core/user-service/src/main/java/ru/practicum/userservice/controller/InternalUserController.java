package ru.practicum.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.userservice.model.User;
import ru.practicum.userservice.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public UserShortDto getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));

        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }

    @PostMapping("/batch")
    public Map<Long, UserShortDto> getUsers(@RequestBody List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId,
                        user -> UserShortDto.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .build()
                ));
    }
}