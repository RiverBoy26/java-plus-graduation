package ru.practicum.eventservice.mapper;

import org.mapstruct.*;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.entity.Category;
import ru.practicum.eventservice.dto.EventFullDto;
import ru.practicum.eventservice.dto.NewEventDto;
import ru.practicum.eventservice.dto.UpdateEventAdminRequest;
import ru.practicum.eventservice.dto.UpdateEventUserRequest;
import ru.practicum.eventservice.model.Event;

/**
 * Маппер для преобразования событий между сущностями и DTO.
 * Использует MapStruct для автоматической генерации реализации.
 * <p>
 * Особенности:
 * <ul>
 *   <li>При создании события устанавливается статус PENDING и текущая дата создания</li>
 *   <li>При обновлении игнорируются null-значения (только переданные поля обновляются)</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface EventMapper {

    /**
     * Преобразование сущности Event в EventFullDto.
     *
     * @param event сущность события
     * @return DTO с полной информацией о событии
     */
    @Mapping(target = "initiator", expression = "java(new ru.practicum.common.dto.UserShortDto(event.getInitiatorId(), event.getInitiatorName()))")
    EventFullDto toFullDto(Event event);

    /**
     * Преобразование сущности Event в EventShortDto.
     *
     * @param event сущность события
     * @return DTO с краткой информацией о событии
     */
    @Mapping(target = "initiator", expression = "java(new ru.practicum.common.dto.UserShortDto(event.getInitiatorId(), event.getInitiatorName()))")
    EventShortDto toShortDto(Event event);

    /**
     * Преобразование NewEventDto в сущность Event.
     *
     * @param dto       данные нового события
     * @param category  категория события
     * @return сущность события со статусом PENDING и созданная в текущий момент
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "initiatorName", ignore = true)
    Event toEntity(NewEventDto dto, Category category);

    /**
     * Обновление события данными от администратора.
     * Обновляются только переданные поля (null игнорируются).
     *
     * @param dto      данные для обновления
     * @param category новая категория (опционально)
     * @param event    обновляемая сущность
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "initiatorName", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "location", source = "dto.location")
    void updateFromAdmin(UpdateEventAdminRequest dto, Category category, @MappingTarget Event event);

    /**
     * Обновление события данными от пользователя.
     * Обновляются только переданные поля (null игнорируются).
     *
     * @param dto      данные для обновления
     * @param category новая категория (опционально)
     * @param event    обновляемая сущность
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "initiatorName", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "location", source = "dto.location")
    void updateFromUser(UpdateEventUserRequest dto, Category category, @MappingTarget Event event);
}