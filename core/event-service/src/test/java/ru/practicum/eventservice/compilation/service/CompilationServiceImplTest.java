
package ru.practicum.eventservice.compilation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.common.dto.CompilationDto;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.eventservice.compilation.CompilationEventReader;
import ru.practicum.eventservice.compilation.dto.NewCompilationDto;
import ru.practicum.eventservice.compilation.dto.UpdateCompilationRequest;
import ru.practicum.eventservice.compilation.entity.Compilation;
import ru.practicum.eventservice.compilation.mapper.CompilationMapper;
import ru.practicum.eventservice.compilation.repository.CompilationRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private CompilationEventReader eventReader;

    @Mock
    private CompilationMapper compilationMapper;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Compilation compilation;
    private CompilationDto compilationDto;
    private NewCompilationDto newCompilationDto;

    @BeforeEach
    void setUp() {
        compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("Летние события");
        compilation.setPinned(true);
        compilation.setEvents(new LinkedHashSet<>());

        compilationDto = new CompilationDto(
                new LinkedHashSet<>(),
                1L,
                true,
                "Летние события"
        );

        newCompilationDto = new NewCompilationDto(
                new LinkedHashSet<>(),
                true,
                "Летние события"
        );
    }

    private EventShortDto eventDto(Long id) {
        EventShortDto dto = new EventShortDto();
        dto.setId(id);
        return dto;
    }

    @Test
    void createCompilation_shouldCreateWithoutEvents() {
        when(compilationMapper.toEntity(
                eq(newCompilationDto), eq(Set.of())
        )).thenReturn(compilation);

        when(compilationRepository.saveAndFlush(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        CompilationDto result =
                compilationService.createCompilation(newCompilationDto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Летние события");
        assertThat(result.getPinned()).isTrue();

        verify(compilationRepository).saveAndFlush(compilation);
        verifyNoInteractions(eventReader);
    }

    @Test
    void createCompilation_shouldCreateWithEvents() {
        newCompilationDto.setEvents(
                new LinkedHashSet<>(List.of(2L, 1L))
        );

        when(eventReader.getEvents(anyList()))
                .thenReturn(List.of(eventDto(2L), eventDto(1L)));

        when(compilationMapper.toEntity(
                eq(newCompilationDto), anySet()
        )).thenReturn(compilation);

        when(compilationRepository.saveAndFlush(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        compilationService.createCompilation(newCompilationDto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> captor =
                ArgumentCaptor.forClass(Set.class);

        verify(compilationMapper).toEntity(
                eq(newCompilationDto), captor.capture()
        );

        assertThat(captor.getValue())
                .containsExactly(1L, 2L);
    }

    @Test
    void createCompilation_whenEventMissing_shouldThrowNotFound() {
        newCompilationDto.setEvents(Set.of(1L, 2L));

        when(eventReader.getEvents(anyList()))
                .thenReturn(List.of(eventDto(1L)));

        assertThatThrownBy(() ->
                compilationService.createCompilation(newCompilationDto)
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("2");

        verify(compilationRepository, never())
                .saveAndFlush(any(Compilation.class));
    }

    @Test
    void createCompilation_whenTitleExists_shouldThrowConflict() {
        when(compilationMapper.toEntity(
                eq(newCompilationDto), anySet()
        )).thenReturn(compilation);

        when(compilationRepository.saveAndFlush(compilation))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate title"
                ));

        assertThatThrownBy(() ->
                compilationService.createCompilation(newCompilationDto)
        )
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Летние события");
    }

    @Test
    void updateCompilation_shouldUpdateAllFields() {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest(
                        Set.of(),
                        false,
                        "Новая подборка"
                );

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(compilationRepository.save(compilation))
                .thenReturn(compilation);

        CompilationDto updatedDto = new CompilationDto(
                Set.of(),
                1L,
                false,
                "Новая подборка"
        );

        when(compilationMapper.toDto(compilation))
                .thenReturn(updatedDto);

        CompilationDto result =
                compilationService.updateCompilation(1L, request);

        assertThat(result.getTitle()).isEqualTo("Новая подборка");
        assertThat(result.getPinned()).isFalse();

        assertThat(compilation.getTitle()).isEqualTo("Новая подборка");
        assertThat(compilation.getPinned()).isFalse();
        assertThat(compilation.getEvents()).isEmpty();

        verify(compilationRepository).save(compilation);
    }

    @Test
    void updateCompilation_shouldUpdateOnlyProvidedFields() {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        request.setPinned(false);

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(compilationRepository.save(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        compilationService.updateCompilation(1L, request);

        assertThat(compilation.getTitle())
                .isEqualTo("Летние события");

        assertThat(compilation.getPinned()).isFalse();
    }

    @Test
    void updateCompilation_shouldReplaceEvents() {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        request.setEvents(
                new LinkedHashSet<>(List.of(2L, 1L))
        );

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(eventReader.getEvents(anyList()))
                .thenReturn(List.of(eventDto(2L), eventDto(1L)));

        when(compilationRepository.save(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        compilationService.updateCompilation(1L, request);

        assertThat(compilation.getEvents())
                .containsExactly(1L, 2L);
    }

    @Test
    void updateCompilation_shouldClearEvents() {
        compilation.setEvents(
                new LinkedHashSet<>(Set.of(1L))
        );

        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        request.setEvents(new LinkedHashSet<>());

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(compilationRepository.save(compilation))
                .thenReturn(compilation);

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        compilationService.updateCompilation(1L, request);

        assertThat(compilation.getEvents()).isEmpty();

        verifyNoInteractions(eventReader);
    }

    @Test
    void updateCompilation_whenCompilationMissing_shouldThrowNotFound() {
        when(compilationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                compilationService.updateCompilation(
                        999L, new UpdateCompilationRequest()
                )
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");

        verify(compilationRepository, never()).save(any());
    }

    @Test
    void updateCompilation_whenEventMissing_shouldThrowNotFound() {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        request.setEvents(Set.of(1L, 2L));

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(eventReader.getEvents(anyList()))
                .thenReturn(List.of(eventDto(1L)));

        assertThatThrownBy(() ->
                compilationService.updateCompilation(1L, request)
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("2");

        verify(compilationRepository, never()).save(any());
    }

    @Test
    void updateCompilation_whenTitleExists_shouldThrowConflict() {
        UpdateCompilationRequest request =
                new UpdateCompilationRequest();

        request.setTitle("Повторяющаяся подборка");

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(compilationRepository.save(compilation))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate title"
                ));

        assertThatThrownBy(() ->
                compilationService.updateCompilation(1L, request)
        )
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Повторяющаяся подборка");
    }

    @Test
    void deleteCompilation_shouldDeleteCompilation() {
        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        compilationService.deleteCompilation(1L);

        verify(compilationRepository).delete(compilation);
    }

    @Test
    void deleteCompilation_whenMissing_shouldThrowNotFound() {
        when(compilationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                compilationService.deleteCompilation(999L)
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");

        verify(compilationRepository, never()).delete(any());
    }

    @Test
    void getCompilations_shouldReturnAll() {
        PageRequest pageable = PageRequest.of(0, 10);
        List<Long> ids = List.of(1L);

        when(compilationRepository.findCompilationIds(
                null, pageable
        )).thenReturn(new PageImpl<>(ids, pageable, 1));

        when(compilationRepository.findAllByIdsWithEvents(ids))
                .thenReturn(List.of(compilation));

        when(compilationMapper.loadEvents(anySet()))
                .thenReturn(Map.of());

        when(compilationMapper.toDto(
                eq(compilation), anyMap()
        )).thenReturn(compilationDto);

        List<CompilationDto> result =
                compilationService.getCompilations(null, pageable);

        assertThat(result).containsExactly(compilationDto);

        verify(compilationRepository)
                .findCompilationIds(null, pageable);

        verify(compilationMapper)
                .toDto(eq(compilation), anyMap());
    }

    @Test
    void getCompilations_shouldFilterByPinned() {
        PageRequest pageable = PageRequest.of(0, 10);
        List<Long> ids = List.of(1L);

        when(compilationRepository.findCompilationIds(
                true, pageable
        )).thenReturn(new PageImpl<>(ids, pageable, 1));

        when(compilationRepository.findAllByIdsWithEvents(ids))
                .thenReturn(List.of(compilation));

        when(compilationMapper.loadEvents(anySet()))
                .thenReturn(Map.of());

        when(compilationMapper.toDto(
                eq(compilation), anyMap()
        )).thenReturn(compilationDto);

        List<CompilationDto> result =
                compilationService.getCompilations(true, pageable);

        assertThat(result).containsExactly(compilationDto);

        verify(compilationRepository)
                .findCompilationIds(true, pageable);
    }

    @Test
    void getCompilation_shouldReturnCompilation() {
        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        when(compilationMapper.toDto(compilation))
                .thenReturn(compilationDto);

        CompilationDto result =
                compilationService.getCompilation(1L);

        assertThat(result).isEqualTo(compilationDto);
    }

    @Test
    void getCompilation_whenMissing_shouldThrowNotFound() {
        when(compilationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                compilationService.getCompilation(999L)
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }
}
