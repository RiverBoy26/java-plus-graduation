package ru.practicum.eventservice.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.eventservice.mapper.EventMapper;
import ru.practicum.eventservice.repository.EventRepository;

import java.util.List;

/** Подборки и мероприятия хранятся в одной БД: обращаемся к репозиторию напрямую. */
@Component
@RequiredArgsConstructor
public class CompilationEventReader {
    private final EventRepository repository;
    private final EventMapper mapper;

    public List<EventShortDto> getEvents(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return repository.findAllById(ids).stream().map(mapper::toShortDto).toList();
    }
}
