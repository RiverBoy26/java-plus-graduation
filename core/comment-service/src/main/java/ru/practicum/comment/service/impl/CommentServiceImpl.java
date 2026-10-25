package ru.practicum.comment.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.feign.EventClient;
import ru.practicum.comment.feign.UserClient;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.comment.service.CommentService;
import ru.practicum.common.dto.CommentDto;
import ru.practicum.common.dto.EventInfoDto;
import ru.practicum.common.dto.NewCommentDto;
import ru.practicum.common.dto.UpdateCommentDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.enums.SortType;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.ForbiddenException;
import ru.practicum.common.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        UserShortDto author = getUserOrThrow(userId);
        EventInfoDto event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException(
                    "Добавлять комментарии можно только к опубликованным событиям"
            );
        }

        Comment comment = commentMapper.toEntity(dto, eventId, userId);

        Comment saved = commentRepository.save(comment);

        return commentMapper.toDto(saved, author);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto) {

        UserShortDto author = getUserOrThrow(userId);

        Comment comment = getCommentEntity(commentId);

        checkAuthor(comment, userId);

        if (Boolean.TRUE.equals(comment.getDeleted())) {
            throw new ConflictException("Удалённый комментарий нельзя редактировать");
        }

        commentMapper.update(dto, comment);
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updated = commentRepository.save(comment);

        return commentMapper.toDto(updated, author);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {

        getUserOrThrow(userId);

        Comment comment = getCommentEntity(commentId);
        checkAuthor(comment, userId);

        if (!comment.getDeleted()) {
            comment.setDeleted(true);
            comment.setUpdatedAt(LocalDateTime.now());
            commentRepository.save(comment);
        }
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size, String sort) {

        checkPublishedEvent(eventId);

        Pageable pageable = createPageable(from, size, sort);

        List<Comment> comments = commentRepository.findByEventIdAndDeletedFalse(eventId, pageable).getContent();

        return mapComments(comments);
    }

    @Override
    public CommentDto getComment(Long eventId, Long commentId) {

        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException(
                        "Комментарий с id=" + commentId + " не найден"));

        if (!comment.getEventId().equals(eventId)) {
            throw new NotFoundException(
                    "Комментарий не принадлежит событию с id=" + eventId);
        }

        checkPublishedEvent(eventId);

        UserShortDto author = getUserOrThrow(comment.getAuthorId());

        return commentMapper.toDto(comment, author);
    }

    @Override
    public List<CommentDto> getUserCommentsByAdmin(Long userId, int from, int size, String sort) {

        getUserOrThrow(userId);

        Pageable pageable = createPageable(from, size, sort);

        List<Comment> comments = commentRepository.findAllByAuthorId(userId, pageable).getContent();

        return mapComments(comments);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {

        Comment comment = getCommentEntity(commentId);
        commentRepository.delete(comment);
    }

    @Override
    public List<CommentDto> getEventCommentsByAdmin(Long eventId, int from, int size, String sort) {

        getEventOrThrow(eventId);

        Pageable pageable = createPageable(from, size, sort);

        List<Comment> comments = commentRepository.findAllByEventId(eventId, pageable).getContent();

        return mapComments(comments);
    }

    private UserShortDto getUserOrThrow(Long userId) {
        try {
            return userClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException(
                    "Пользователь с id=" + userId + " не найден"
            );
        }
    }

    private EventInfoDto getEventOrThrow(Long eventId) {
        try {
            return eventClient.getEvent(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException(
                    "Событие с id=" + eventId + " не найдено"
            );
        }
    }

    private Comment getCommentEntity(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        "Комментарий с id=" + commentId + " не найден"));
    }

    private void checkAuthor(Comment comment, Long userId) {
        if (!comment.getAuthorId().equals(userId)) {
            throw new ForbiddenException(
                    "Пользователь с id=" + userId
                            + " не является автором комментария с id="
                            + comment.getId());
        }
    }

    private void checkPublishedEvent(Long eventId) {
        EventInfoDto event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException(
                    "Опубликованное событие с id="
                            + eventId + " не найдено");
        }
    }

    private Pageable createPageable(int from, int size, String sort) {
        if (from < 0 || size <= 0) {
            throw new IllegalArgumentException(
                    "from должен быть >= 0, size должен быть > 0");
        }

        if (sort != null
                && sort.equalsIgnoreCase(SortType.VIEWS.name())) {
            return PageRequest.of(from / size, size);
        }

        return PageRequest.of(
                from / size,
                size,
                Sort.by("createdAt").ascending());
    }

    private List<CommentDto> mapComments(List<Comment> comments) {

        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserShortDto> authors = userClient.getUsers(authorIds);

        return comments.stream()
                .map(comment -> {
                    UserShortDto author = authors.get(comment.getAuthorId());

                    if (author == null) {
                        author = UserShortDto.builder()
                                .id(comment.getAuthorId())
                                .name("Удалённый пользователь")
                                .build();
                    }

                    return commentMapper.toDto(comment, author);
                })
                .collect(Collectors.toList());
    }
}
