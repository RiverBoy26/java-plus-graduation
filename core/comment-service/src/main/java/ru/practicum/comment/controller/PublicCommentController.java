package ru.practicum.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.comment.service.CommentService;
import ru.practicum.common.dto.CommentDto;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/events/{eventId}/comments")
public class PublicCommentController implements PublicCommentControllerApi {
    private final CommentService service;

    @Override
    public List<CommentDto> getComments(Long eventId, int from, int size, String sort) {
        log.info("GET /events/{}/comments?from={}&size={}&sort={}", eventId, from, size, sort);
        return service.getEventComments(eventId, from, size, sort);
    }

    @Override
    public CommentDto getComment(Long eventId, Long commentId) {
        log.info("GET /events/{}/comments/{}", eventId, commentId);
        return service.getComment(eventId, commentId);
    }
}
