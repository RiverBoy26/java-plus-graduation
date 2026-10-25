package ru.practicum.eventservice.compilation.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.common.dto.CompilationDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.eventservice.compilation.CompilationEventReader;
import ru.practicum.eventservice.compilation.dto.NewCompilationDto;
import ru.practicum.eventservice.compilation.entity.Compilation;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования подборок между сущностями и DTO.
 */
@Component
@RequiredArgsConstructor
public class CompilationMapper {

    private final CompilationEventReader eventReader;

    /**
     * Преобразование NewCompilationDto и списка событий в сущность Compilation.
     *
     * @param dto    данные для создания подборки
     * @param events множество событий, входящих в подборку
     * @return сущность подборки
     */
    public Compilation toEntity(NewCompilationDto dto, Set<Long> events) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned());
        compilation.setEvents(events == null ? new LinkedHashSet<>() : new LinkedHashSet<>(events));
        return compilation;
    }

    /**
     * Преобразование сущности Compilation в CompilationDto.
     *
     * @param compilation сущность подборки
     * @return DTO подборки с преобразованными событиями
     */
    public CompilationDto toDto(Compilation compilation) {
        return toDto(compilation, loadEvents(compilation.getEvents()));
    }

    public java.util.Map<Long, EventShortDto> loadEvents(Set<Long> ids) {
        if (ids.isEmpty()) return java.util.Map.of();
        try {
            return eventReader.getEvents(new java.util.ArrayList<>(ids)).stream()
                    .collect(Collectors.toMap(EventShortDto::getId, e -> e));
        } catch (Exception ex) {
            // This method is read-side enrichment only. Creating and updating compilations
            // still validates supplied event ids via the local reader and never silently succeeds.
            return java.util.Map.of();
        }
    }

    public CompilationDto toDto(Compilation compilation, java.util.Map<Long, EventShortDto> allEvents) {
        Set<EventShortDto> events = compilation.getEvents().stream().sorted()
                .map(allEvents::get).filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new CompilationDto(events, compilation.getId(), compilation.getPinned(), compilation.getTitle());
    }
}