package ru.practicum.eventservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.EventInfoDto;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.eventservice.model.Event;
import ru.practicum.eventservice.repository.EventRepository;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events")
public class InternalEventController {

    private final EventRepository eventRepository;

    @GetMapping("/{id}")
    public EventInfoDto getEvent(@PathVariable Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));

        EventInfoDto dto = new EventInfoDto();
        dto.setId(event.getId());
        dto.setInitiatorId(event.getInitiatorId());
        dto.setState(event.getState());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());

        return dto;
    }
}