package ru.practicum.eventservice.compilation.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Сущность подборки событий.
 * <p>
 * Подборка - это набор событий, объединенных общей темой.
 * Подборки могут быть закреплены на главной странице (pinned = true).
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "compilations", uniqueConstraints = @UniqueConstraint(name = "uq_compilation_name", columnNames = "title"))
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_seq")
    @SequenceGenerator(name = "entity_seq", sequenceName = "entity_seq", allocationSize = 1)
    private Long id;

    /**
     * Список событий, входящих в подборку.
     * Связь многие-ко-многим с таблицей events через промежуточную таблицу compilation_events.
     * Сортировка событий по идентификатору в порядке возрастания.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "compilation_events", joinColumns = @JoinColumn(name = "compilation_id"))
    @Column(name = "event_id", nullable = false)
    private Set<Long> events = new LinkedHashSet<>();

    /**
     * Флаг закрепления подборки на главной странице.
     * Если true - подборка отображается на главной странице сайта.
     */
    @Column(name = "pinned", nullable = false)
    private Boolean pinned = false;

    /**
     * Название подборки. Максимальная длина 50 символов.
     */
    @Column(name = "title", nullable = false, length = 50)
    private String title;
}