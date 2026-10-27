package ru.practicum.comment.mapper;

import org.mapstruct.*;
import ru.practicum.comment.model.Comment;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.common.dto.NewCommentDto;
import ru.practicum.common.dto.UpdateCommentDto;
import ru.practicum.common.dto.UserShortDto;


import java.util.List;

/**
 * Маппер для преобразования комментариев между сущностями и DTO.
 * Использует MapStruct для автоматической генерации реализации.
 * <p>
 * Особенности:
 * <ul>
 *   <li>При создании комментария устанавливается текущая дата создания</li>
 *   <li>Новый комментарий по умолчанию считается неудалённым</li>
 *   <li>При обновлении игнорируются null-значения</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface CommentMapper {

    /**
     * Преобразование сущности Comment в CommentDto.
     *
     * @param comment сущность комментария
     * @return DTO с информацией о комментарии
     */
    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "eventId", source = "comment.eventId")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "isDeleted", source = "comment.deleted")
    CommentDto toDto(Comment comment, UserShortDto author);

    /**
     * Преобразование списка сущностей Comment в список CommentDto.
     *
     * @param comments список сущностей комментариев
     * @return список DTO с информацией о комментариях
     */
    List<CommentDto> toDtoList(List<Comment> comments);

    /**
     * Преобразование NewCommentDto в сущность Comment.
     *
     * @param dto    данные нового комментария
     * @param eventId  ID события, к которому относится комментарий
     * @param authorId ID автора комментария
     * @return сущность комментария с текущей датой создания
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "authorId", source = "authorId")
    @Mapping(target = "createdAt", expression =
            "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    Comment toEntity(NewCommentDto dto, Long eventId, Long authorId);

    /**
     * Обновление комментария переданными пользователем данными.
     * Обновляются только переданные поля (null игнорируются).
     *
     * @param dto     данные для обновления
     * @param comment обновляемая сущность комментария
     */
    @BeanMapping(nullValuePropertyMappingStrategy =
            NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void update(UpdateCommentDto dto, @MappingTarget Comment comment);
}
