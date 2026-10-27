package ru.practicum.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.userservice.dto.NewUserRequest;
import ru.practicum.userservice.dto.UserDto;
import ru.practicum.userservice.model.User;

/**
 * Маппер для преобразования пользователей между сущностями и DTO.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Преобразование сущности User в UserDto (полная информация).
     *
     * @param user сущность пользователя
     * @return DTO с полной информацией о пользователе
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "name", source = "name")
    UserDto toDto(User user);

    /**
     * Преобразование сущности User в UserShortDto (краткая информация).
     *
     * @param user сущность пользователя
     * @return DTO с краткой информацией о пользователе
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    UserShortDto toShortDto(User user);

    /**
     * Преобразование NewUserRequest в сущность User.
     * Идентификатор игнорируется (генерируется БД).
     *
     * @param dto данные нового пользователя
     * @return сущность пользователя
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "name", source = "name")
    User toEntity(NewUserRequest dto);
}