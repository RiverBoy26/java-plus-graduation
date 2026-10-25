
package ru.practicum.eventservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.eventservice.category.service.CategoryService;
import ru.practicum.eventservice.feign.RequestClient;
import ru.practicum.eventservice.feign.UserClient;
import ru.practicum.eventservice.mapper.EventMapper;
import ru.practicum.eventservice.model.Event;
import ru.practicum.eventservice.repository.EventRepository;
import ru.practicum.eventservice.repository.EventRepositoryCustom;
import ru.practicum.statistics.client.controller.StatsClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository repository;

    @Mock
    private EventMapper mapper;

    @Mock
    private StatsClient statsClient;

    @Mock
    private UserClient userClient;

    @Mock
    private CategoryService categoryService;

    @Mock
    private EventRepositoryCustom eventRepositoryCustom;

    @Mock
    private RequestClient requestClient;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void getEventEntity_existingEvent() {
        Event event = new Event();
        event.setId(5L);
        event.setInitiatorId(1L);
        event.setInitiatorName("Иван");

        when(repository.findById(5L))
                .thenReturn(Optional.of(event));

        assertThat(eventService.getEventEntity(5L))
                .isSameAs(event);

        verify(repository).findById(5L);
    }

    @Test
    void getEventEntity_missingEvent_throwsNotFound() {
        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> eventService.getEventEntity(999L)
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPublicEvent_unpublishedEvent_throwsNotFound() {
        Event event = new Event();
        event.setId(5L);
        event.setInitiatorId(1L);
        event.setInitiatorName("Иван");
        event.setState(EventState.PENDING);

        when(repository.findById(5L))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(
                () -> eventService.getPublicEvent(5L)
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    void existsById_delegatesToRepository() {
        when(repository.existsById(5L))
                .thenReturn(true);

        assertThat(eventService.existsById(5L)).isTrue();
    }

    @Test
    void findAllByIds_emptyIds_returnsEmptyList() {
        assertThat(eventService.findAllByIds(List.of()))
                .isEmpty();

        assertThat(eventService.findAllByIds(null))
                .isEmpty();
    }

    @Test
    void findAllByIds_existingIds_returnsEvents() {
        Event event = new Event();
        event.setId(5L);

        when(repository.findAllById(List.of(5L)))
                .thenReturn(List.of(event));

        assertThat(eventService.findAllByIds(List.of(5L)))
                .containsExactly(event);
    }

    @Test
    void getAdminEvents_emptyPage_returnsEmptyList() {
        when(eventRepositoryCustom.findAllByAdminFilters(
                isNull(), isNull(), isNull(),
                isNull(), isNull(), any()
        )).thenReturn(new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
        ));

        assertThat(eventService.getAdminEvents(
                null, null, null, null, null, 0, 10
        )).isEmpty();
    }
}
