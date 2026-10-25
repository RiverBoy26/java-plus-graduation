package ru.practicum.requestservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.common.enums.RequestStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Сущность запроса на участие в событии.
 * <p>
 * Запрос создается пользователем для участия в событии.
 * Может находиться в одном из статусов: PENDING, CONFIRMED, REJECTED, CANCELED.
 * </p>
 */
@Entity
@Table(name = "participation_requests", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "requester_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Событие, на которое подается запрос.
     * Связь многие-к-одному с таблицей events.
     */
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    /**
     * Пользователь, подавший запрос на участие.
     * Связь многие-к-одному с таблицей users.
     */
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    /**
     * Текущий статус запроса.
     * Возможные значения: PENDING, CONFIRMED, REJECTED, CANCELED.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    /**
     * Дата и время создания запроса.
     */
    @Column(nullable = false)
    private LocalDateTime created;
}