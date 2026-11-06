package ru.practicum.analyzer.useraction.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.analyzer.useraction.model.UserAction;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    List<UserAction> findByUserIdAndTimestampBetweenOrderByTimestampDesc(
            Long userId,
            Instant from,
            Instant to,
            Pageable pageable
    );

    List<UserAction> findByUserIdAndEventIdIn(
            long userId,
            List<Long> eventIds
    );

    Optional<UserAction> findByUserIdAndEventId(long userId, long eventId);

    List<UserAction> findByUserIdOrderByTimestampDesc(
            long userId,
            Pageable pageable
    );


    Set<Long> findEventIdsByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(u.rating), 0) FROM UserAction u WHERE u.eventId = :eventId")
    Double sumRatingByEventId(Long eventId);
}