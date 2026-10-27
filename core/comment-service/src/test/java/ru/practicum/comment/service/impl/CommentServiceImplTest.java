
package ru.practicum.comment.service.impl;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.comment.feign.EventClient;
import ru.practicum.comment.feign.UserClient;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.common.dto.*;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.ForbiddenException;
import ru.practicum.common.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {
    private static final long USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long EVENT_ID = 10L;
    private static final long COMMENT_ID = 100L;

    @Mock private CommentRepository commentRepository;
    @Mock private UserClient userClient;
    @Mock private EventClient eventClient;
    @Mock private CommentMapper commentMapper;
    @InjectMocks private CommentServiceImpl commentService;

    private UserShortDto author;
    private EventInfoDto publishedEvent;
    private Comment comment;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        author = UserShortDto.builder()
                .id(USER_ID)
                .name("Автор")
                .build();

        publishedEvent = new EventInfoDto();
        publishedEvent.setId(EVENT_ID);
        publishedEvent.setState(EventState.PUBLISHED);

        comment = Comment.builder()
                .id(COMMENT_ID)
                .eventId(EVENT_ID)
                .authorId(USER_ID)
                .text("Исходный текст")
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();

        commentDto = CommentDto.builder()
                .id(COMMENT_ID)
                .eventId(EVENT_ID)
                .author(author)
                .text(comment.getText())
                .isDeleted(false)
                .build();
    }

    @Test
    void createComment_shouldReturnSavedDto() {
        NewCommentDto request = NewCommentDto.builder()
                .text("Новый комментарий")
                .build();

        Comment created = Comment.builder()
                .eventId(EVENT_ID)
                .authorId(USER_ID)
                .text(request.getText())
                .deleted(false)
                .build();

        when(userClient.getUser(USER_ID)).thenReturn(author);
        when(eventClient.getEvent(EVENT_ID))
                .thenReturn(publishedEvent);
        when(commentMapper.toEntity(request, EVENT_ID, USER_ID))
                .thenReturn(created);
        when(commentRepository.save(created)).thenReturn(comment);
        when(commentMapper.toDto(comment, author))
                .thenReturn(commentDto);

        CommentDto result = commentService.createComment(
                USER_ID, EVENT_ID, request
        );

        assertThat(result.getId()).isEqualTo(COMMENT_ID);
        verify(commentRepository).save(created);
    }

    @Test
    void createComment_whenEventIsPending_shouldThrowConflict() {
        EventInfoDto pending = new EventInfoDto();
        pending.setId(EVENT_ID);
        pending.setState(EventState.PENDING);

        when(userClient.getUser(USER_ID)).thenReturn(author);
        when(eventClient.getEvent(EVENT_ID)).thenReturn(pending);

        assertThatThrownBy(() -> commentService.createComment(
                USER_ID,
                EVENT_ID,
                NewCommentDto.builder().text("Текст").build()
        )).isInstanceOf(ConflictException.class);

        verifyNoInteractions(commentRepository);
    }

    @Test
    void updateComment_shouldReturnUpdatedDto() {
        UpdateCommentDto request = UpdateCommentDto.builder()
                .text("Новый текст")
                .build();

        when(userClient.getUser(USER_ID)).thenReturn(author);
        when(commentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toDto(comment, author))
                .thenReturn(commentDto);

        CommentDto result = commentService.updateComment(
                USER_ID, COMMENT_ID, request
        );

        assertThat(result).isSameAs(commentDto);
        assertThat(comment.getUpdatedAt()).isNotNull();
        verify(commentMapper).update(request, comment);
    }

    @Test
    void updateComment_byOtherUser_shouldThrowForbidden() {
        when(userClient.getUser(OTHER_USER_ID))
                .thenReturn(UserShortDto.builder()
                        .id(OTHER_USER_ID)
                        .name("Другой")
                        .build());

        when(commentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(
                OTHER_USER_ID,
                COMMENT_ID,
                UpdateCommentDto.builder().text("Текст").build()
        )).isInstanceOf(ForbiddenException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_whenDeleted_shouldThrowConflict() {
        comment.setDeleted(true);

        when(userClient.getUser(USER_ID)).thenReturn(author);
        when(commentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(
                USER_ID,
                COMMENT_ID,
                UpdateCommentDto.builder().text("Текст").build()
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteCommentByUser_shouldSoftDelete() {
        when(userClient.getUser(USER_ID)).thenReturn(author);
        when(commentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(comment));

        commentService.deleteCommentByUser(USER_ID, COMMENT_ID);

        assertThat(comment.getDeleted()).isTrue();
        assertThat(comment.getUpdatedAt()).isNotNull();

        verify(commentRepository).save(comment);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteCommentByUser_byOtherUser_shouldThrowForbidden() {
        when(userClient.getUser(OTHER_USER_ID))
                .thenReturn(UserShortDto.builder()
                        .id(OTHER_USER_ID)
                        .name("Другой")
                        .build());

        when(commentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() ->
                commentService.deleteCommentByUser(
                        OTHER_USER_ID, COMMENT_ID
                )
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteCommentByAdmin_shouldDeleteEntity() {
        when(commentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(comment));

        commentService.deleteCommentByAdmin(COMMENT_ID);

        verify(commentRepository).delete(comment);
    }

    @Test
    void getComment_shouldReturnDto() {
        when(commentRepository.findByIdAndDeletedFalse(COMMENT_ID))
                .thenReturn(Optional.of(comment));
        when(eventClient.getEvent(EVENT_ID))
                .thenReturn(publishedEvent);
        when(userClient.getUser(USER_ID))
                .thenReturn(author);
        when(commentMapper.toDto(comment, author))
                .thenReturn(commentDto);

        assertThat(commentService.getComment(EVENT_ID, COMMENT_ID))
                .isSameAs(commentDto);
    }

    @Test
    void getComment_whenMissing_shouldThrowNotFound() {
        when(commentRepository.findByIdAndDeletedFalse(COMMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                commentService.getComment(EVENT_ID, COMMENT_ID)
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getEventComments_shouldLoadAuthorsInBatch() {
        when(eventClient.getEvent(EVENT_ID))
                .thenReturn(publishedEvent);
        when(commentRepository.findByEventIdAndDeletedFalse(
                eq(EVENT_ID), any()
        )).thenReturn(new PageImpl<>(List.of(comment)));

        when(userClient.getUsers(List.of(USER_ID)))
                .thenReturn(Map.of(USER_ID, author));
        when(commentMapper.toDto(comment, author))
                .thenReturn(commentDto);

        assertThat(commentService.getEventComments(
                EVENT_ID, 0, 10, "createdAt"
        )).containsExactly(commentDto);

        verify(userClient).getUsers(List.of(USER_ID));
    }

    @Test
    void getEventComments_whenEmpty_shouldNotCallUsers() {
        when(eventClient.getEvent(EVENT_ID))
                .thenReturn(publishedEvent);
        when(commentRepository.findByEventIdAndDeletedFalse(
                eq(EVENT_ID), any()
        )).thenReturn(new PageImpl<>(List.of()));

        assertThat(commentService.getEventComments(
                EVENT_ID, 0, 10, "createdAt"
        )).isEmpty();

        verifyNoInteractions(userClient);
    }

    @Test
    void getUserCommentsByAdmin_shouldLoadAuthors() {
        when(userClient.getUser(USER_ID)).thenReturn(author);
        when(commentRepository.findAllByAuthorId(
                eq(USER_ID), any()
        )).thenReturn(new PageImpl<>(List.of(comment)));

        when(userClient.getUsers(List.of(USER_ID)))
                .thenReturn(Map.of(USER_ID, author));
        when(commentMapper.toDto(comment, author))
                .thenReturn(commentDto);

        assertThat(commentService.getUserCommentsByAdmin(
                USER_ID, 0, 10, "createdAt"
        )).containsExactly(commentDto);
    }

    @Test
    void getEventCommentsByAdmin_shouldReturnEmptyList() {
        when(eventClient.getEvent(EVENT_ID))
                .thenReturn(publishedEvent);
        when(commentRepository.findAllByEventId(
                eq(EVENT_ID), any()
        )).thenReturn(new PageImpl<>(List.of()));

        assertThat(commentService.getEventCommentsByAdmin(
                EVENT_ID, 0, 10, "createdAt"
        )).isEmpty();
    }

    @Test
    void missingUserFromFeign_shouldPropagateFeign404UnlessServiceTranslatesIt() {
        when(userClient.getUser(USER_ID))
                .thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() ->
                commentService.deleteCommentByUser(
                        USER_ID, COMMENT_ID
                )
        ).isInstanceOfAny(
                FeignException.NotFound.class,
                NotFoundException.class
        );
    }
}
