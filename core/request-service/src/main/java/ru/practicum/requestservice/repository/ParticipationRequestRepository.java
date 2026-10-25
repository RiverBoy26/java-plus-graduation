package ru.practicum.requestservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.common.enums.RequestStatus;
import ru.practicum.requestservice.entity.ParticipationRequest;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с запросами на участие в событиях.
 * Предоставляет методы для поиска запросов по пользователям, событиям и статусам.
 */
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    interface ConfirmedCount {
        Long getEventId();
        Long getConfirmed();
    }

    @Query("SELECT p.eventId AS eventId, COUNT(p) AS confirmed FROM ParticipationRequest p " +
            "WHERE p.eventId IN :ids AND p.status = :status GROUP BY p.eventId")
    List<ConfirmedCount> confirmedCounts(@Param("ids") List<Long> ids, @Param("status") RequestStatus status);


    /**
     * Поиск всех запросов пользователя по его идентификатору.
     *
     * @param userId идентификатор пользователя
     * @return список запросов пользователя
     */
    List<ParticipationRequest> findByRequesterId(Long userId);

    /**
     * Поиск запроса по идентификатору события и пользователя.
     *
     * @param eventId идентификатор события
     * @param userId  идентификатор пользователя
     * @return Optional с запросом, если найден
     */
    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long userId);

    /**
     * Поиск всех запросов для конкретного события.
     *
     * @param eventId идентификатор события
     * @return список запросов для события
     */
    List<ParticipationRequest> findByEventId(Long eventId);

    /**
     * Подсчет количества запросов для события с определенным статусом.
     *
     * @param eventId идентификатор события
     * @param status  статус запроса
     * @return количество запросов
     */
    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    /**
     * Поиск запросов для события по списку идентификаторов.
     *
     * @param eventId идентификатор события
     * @param ids     список идентификаторов запросов
     * @return список запросов
     */
    List<ParticipationRequest> findByEventIdAndIdIn(Long eventId, List<Long> ids);

    /**
     * Поиск всех запросов для события с определенным статусом.
     *
     * @param eventId идентификатор события
     * @param status  статус запроса
     * @return список запросов с указанным статусом
     */
    List<ParticipationRequest> findByEventIdAndStatus(Long eventId, RequestStatus status);
}