
package ru.practicum.eventservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.common.entity.Category;
import ru.practicum.common.enums.AdminStateAction;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.ValidationException;
import ru.practicum.eventservice.category.service.CategoryService;
import ru.practicum.eventservice.dto.EventFullDto;
import ru.practicum.eventservice.dto.NewEventDto;
import ru.practicum.eventservice.dto.UpdateEventAdminRequest;
import ru.practicum.eventservice.dto.UpdateEventUserRequest;
import ru.practicum.eventservice.feign.RequestClient;
import ru.practicum.eventservice.feign.UserClient;
import ru.practicum.eventservice.mapper.EventMapper;
import ru.practicum.eventservice.model.Event;
import ru.practicum.eventservice.repository.EventRepository;
import ru.practicum.eventservice.repository.EventRepositoryCustom;
import ru.practicum.eventservice.utils.EventUpdateUtils;
import ru.practicum.ewm.stats.service.dashboard.RecommendedEventProto;
import ru.practicum.statistics.client.controller.AnalyzerClient;
import ru.practicum.statistics.client.controller.CollectorClient;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 10;

    private final EventRepository repository;
    private final EventMapper mapper;
    private final CollectorClient collectorClient;
    private final AnalyzerClient analyzerClient;
    private final UserClient userClient;
    private final CategoryService categoryService;
    private final EventRepositoryCustom eventRepositoryCustom;
    private final RequestClient requestClient;
    private final JdbcTemplate jdbcTemplate;

    // ==================== ADMIN ====================

    @Override
    public List<EventFullDto> getAdminEvents(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size
    ) {
        boolean findWithConfirmedRequests = false;

        if (users != null && users.contains(0L)) {
            findWithConfirmedRequests = true;
            users = null;
        }

        if (categories != null && categories.contains(0L)) {
            categories = null;
        }

        log.info(
                "getAdminEvents: users={}, states={}, categories={}, " +
                        "rangeStart={}, rangeEnd={}, from={}, size={}",
                users, states, categories,
                rangeStart, rangeEnd, from, size
        );

        Pageable pageable = PageRequest.of(from / size, size);

        Page<Event> page = eventRepositoryCustom.findAllByAdminFilters(
                users,
                states,
                categories,
                rangeStart,
                rangeEnd,
                pageable
        );

        if (page == null || page.getContent().isEmpty()) {
            return List.of();
        }

        List<Event> events = page.getContent();
        Map<Long, Long> counts = countsFor(events);

        if (findWithConfirmedRequests) {
            events = events.stream()
                    .filter(event ->
                            counts.getOrDefault(event.getId(), 0L) > 0)
                    .toList();
        }

        Map<Long, Double> ratings = ratingsFor(events);

        return events.stream()
                .map(event -> enrichWithRatingAndRequests(
                        event, counts, ratings))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(
            Long eventId,
            UpdateEventAdminRequest request
    ) {
        log.info(
                "Updating event by admin: eventId={}, request={}",
                eventId, request
        );

        Event event = getEventEntity(eventId);

        if (request.getStateAction() ==
                AdminStateAction.PUBLISH_EVENT) {

            if (event.getState() != EventState.PENDING) {
                throw new ConflictException(
                        "Cannot publish event because it is not PENDING"
                );
            }

            if (request.getEventDate() != null &&
                    request.getEventDate().isBefore(
                            LocalDateTime.now().plusHours(1))) {

                throw new ConflictException(
                        "Event date must be at least 1 hour from now"
                );
            }
        }

        if (request.getStateAction() ==
                AdminStateAction.REJECT_EVENT) {

            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException(
                        "Cannot reject published event"
                );
            }
        }

        if (request.getParticipantLimit() != null &&
                request.getParticipantLimit() < 0) {

            throw new BadRequestException(
                    "Participant limit cannot be negative"
            );
        }

        Category category = null;

        if (request.getCategory() != null) {
            category = getCategoryEntity(
                    request.getCategory()
            );
        }

        EventUpdateUtils.updateFromAdmin(
                event, request, category
        );

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case PUBLISH_EVENT:
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;

                case REJECT_EVENT:
                    event.setState(EventState.CANCELED);
                    break;

                default:
                    break;
            }
        }

        event = repository.save(event);

        return enrichWithRatingAndRequests(event);
    }

    // ==================== PRIVATE ====================

    @Override
    public List<EventShortDto> getUserEvents(
            Long userId,
            int from,
            int size
    ) {
        log.info(
                "Getting events for user: userId={}",
                userId
        );

        getUserEntity(userId);

        Pageable pageable = PageRequest.of(
                from / size, size
        );

        Page<Event> page = repository.findByInitiatorId(
                userId, pageable
        );

        List<Event> events = page.getContent();

        Map<Long, Long> counts = countsFor(events);
        Map<Long, Double> ratings = ratingsFor(events);

        return events.stream()
                .map(event -> shortDto(
                        event, counts, ratings))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto createEvent(
            Long userId,
            NewEventDto dto
    ) {
        log.info(
                "Creating event: userId={}, dto={}",
                userId, dto
        );

        if (dto.getDescription() == null ||
                dto.getDescription().isBlank()) {

            throw new ValidationException(
                    "Description must not be blank"
            );
        }

        if (dto.getAnnotation() == null ||
                dto.getAnnotation().isBlank()) {

            throw new ValidationException(
                    "Annotation must not be blank"
            );
        }

        if (dto.getParticipantLimit() != null &&
                dto.getParticipantLimit() < 0) {

            throw new BadRequestException(
                    "Participant limit cannot be negative"
            );
        }

        UserShortDto user = getUserEntity(userId);

        Category category = getCategoryEntity(
                dto.getCategory()
        );

        if (dto.getEventDate().isBefore(
                LocalDateTime.now().plusHours(2))) {

            throw new BadRequestException(
                    "Event date must be at least 2 hours from now"
            );
        }

        Event event = mapper.toEntity(
                dto, category
        );

        event.setInitiatorId(user.getId());
        event.setInitiatorName(user.getName());
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        event.setPaid(
                dto.getPaid() != null ? dto.getPaid() : false
        );

        event.setParticipantLimit(
                dto.getParticipantLimit() != null
                        ? dto.getParticipantLimit()
                        : 0
        );

        event.setRequestModeration(
                dto.getRequestModeration() != null
                        ? dto.getRequestModeration()
                        : true
        );

        event = repository.save(event);

        log.info(
                "Created event with id={}",
                event.getId()
        );

        EventFullDto result = mapper.toFullDto(event);

        result.setConfirmedRequests(0L);
        result.setRating(0.0);

        return result;
    }

    @Override
    public EventFullDto getUserEvent(
            Long userId,
            Long eventId
    ) {
        log.info(
                "Getting user event: userId={}, eventId={}",
                userId, eventId
        );

        getUserEntity(userId);

        Event event = getEventEntity(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException(
                    "Event with id " + eventId +
                            " not found for user " + userId
            );
        }

        return enrichWithRatingAndRequests(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(
            Long userId,
            Long eventId,
            UpdateEventUserRequest request
    ) {
        log.info(
                "Updating user event: userId={}, eventId={}",
                userId, eventId
        );

        getUserEntity(userId);

        Event event = getEventEntity(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException(
                    "Event with id " + eventId +
                            " not found for user " + userId
            );
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException(
                    "Only pending or canceled events can be changed"
            );
        }

        if (request.getEventDate() != null &&
                request.getEventDate().isBefore(
                        LocalDateTime.now().plusHours(2))) {

            throw new BadRequestException(
                    "Event date must be at least 2 hours from now"
            );
        }

        if (request.getParticipantLimit() != null &&
                request.getParticipantLimit() < 0) {

            throw new BadRequestException(
                    "Participant limit cannot be negative"
            );
        }

        Category category = null;

        if (request.getCategory() != null) {
            category = getCategoryEntity(
                    request.getCategory()
            );
        }

        EventUpdateUtils.updateFromUser(
                event, request, category
        );

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW ->
                        event.setState(EventState.PENDING);

                case CANCEL_REVIEW ->
                        event.setState(EventState.CANCELED);

                default ->
                        throw new BadRequestException(
                                "Unknown state action"
                        );
            }
        }

        event = repository.save(event);

        return enrichWithRatingAndRequests(event);
    }

    // ==================== PUBLIC ====================

    @Override
    public List<EventShortDto> getPublicEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            String sort,
            int from,
            int size
    ) {
        log.info(
                "Getting public events: text={}, categories={}, " +
                        "paid={}, rangeStart={}, rangeEnd={}, " +
                        "onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid,
                rangeStart, rangeEnd,
                onlyAvailable, sort, from, size
        );

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        if (rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException(
                    "Range start must not be after range end"
            );
        }

        if (text == null) {
            text = "";
        }

        boolean sortByRating =
                "RATING".equalsIgnoreCase(sort);

        Pageable pageable;

        if (sortByRating) {
            pageable = PageRequest.of(from / size, size);
        } else {
            pageable = PageRequest.of(
                    from / size,
                    size,
                    Sort.by("eventDate").ascending()
            );
        }

        Page<Event> page =
                eventRepositoryCustom.findPublishedEvents(
                        text,
                        categories,
                        paid,
                        rangeStart,
                        rangeEnd,
                        pageable
                );

        List<Event> events = page.getContent();

        Map<Long, Long> confirmed = countsFor(events);

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.stream()
                    .filter(event ->
                            event.getParticipantLimit() == 0 ||
                                    confirmed.getOrDefault(
                                            event.getId(), 0L
                                    ) < event.getParticipantLimit())
                    .toList();
        }

        Map<Long, Double> ratings = ratingsFor(events);

        if (sortByRating) {
            events = events.stream()
                    .sorted(
                            Comparator.comparingDouble(
                                    (Event event) ->
                                            ratings.getOrDefault(
                                                    event.getId(), 0.0
                                            )
                            ).reversed()
                    )
                    .toList();
        }

        // GET /events не отправляет ACTION_VIEW
        return events.stream()
                .map(event -> shortDto(
                        event, confirmed, ratings))
                .toList();
    }

    /**
     * Получение опубликованного мероприятия
     * для внутренних вызовов без регистрации просмотра.
     */
    @Override
    public EventFullDto getPublicEvent(Long eventId) {
        Event event = getPublishedEvent(eventId);

        return enrichWithRatingAndRequests(event);
    }

    /**
     * GET /events/{id}
     *
     * Отправка ACTION_VIEW в Collector.
     */
    @Override
    @Transactional
    public EventFullDto getPublicEvent(
            Long eventId,
            long userId
    ) {
        log.info(
                "Getting public event: eventId={}, userId={}",
                eventId, userId
        );

        Event event = getPublishedEvent(eventId);

        collectorClient.sendView(userId, eventId);

        // Сохраняем факт просмотра для проверки лайков.
        jdbcTemplate.update(
                """
                INSERT INTO event_views (user_id, event_id)
                VALUES (?, ?)
                ON CONFLICT (user_id, event_id) DO NOTHING
                """,
                userId,
                eventId
        );

        log.info(
                "User {} viewed event {}",
                userId, eventId
        );

        return enrichWithRatingAndRequests(event);
    }

    // ==================== RECOMMENDATIONS ====================

    /**
     * GET /events/recommendations
     *
     * Получение рекомендаций от Analyzer.
     */
    @Override
    public List<EventShortDto> getRecommendations(
            long userId,
            int maxResults
    ) {
        log.info(
                "Getting recommendations: userId={}, maxResults={}",
                userId, maxResults
        );

        if (maxResults <= 0) {
            throw new BadRequestException(
                    "maxResults must be positive"
            );
        }

        Map<Long, Double> scores =
                analyzerClient.getRecommendations(
                        userId, maxResults
                ).collect(
                        Collectors.toMap(
                                RecommendedEventProto::getEventId,
                                RecommendedEventProto::getScore,
                                Math::max
                        )
                );

        if (scores.isEmpty()) {
            return List.of();
        }

        List<Event> events = repository.findAllById(
                scores.keySet()
        );

        events = events.stream()
                .filter(event ->
                        event.getState() == EventState.PUBLISHED)
                .sorted(
                        Comparator.comparingDouble(
                                (Event event) ->
                                        scores.getOrDefault(
                                                event.getId(), 0.0
                                        )
                        ).reversed()
                )
                .limit(maxResults)
                .toList();

        Map<Long, Long> counts = countsFor(events);
        Map<Long, Double> ratings = ratingsFor(events);

        return events.stream()
                .map(event -> shortDto(
                        event, counts, ratings))
                .toList();
    }

    // ==================== LIKES ====================

    /**
     * PUT /events/{eventId}/like
     *
     * Лайк доступен только после просмотра мероприятия.
     */
    @Override
    public void likeEvent(
            Long eventId,
            long userId
    ) {
        log.info(
                "User {} likes event {}",
                userId, eventId
        );

        getPublishedEvent(eventId);

        Integer views = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM event_views
                WHERE user_id = ?
                  AND event_id = ?
                """,
                Integer.class,
                userId,
                eventId
        );

        if (views == null || views == 0) {
            throw new BadRequestException(
                    "Only viewed events can be liked"
            );
        }

        collectorClient.sendLike(userId, eventId);

        log.info(
                "Like sent to Collector: userId={}, eventId={}",
                userId, eventId
        );
    }

    // ==================== HELPERS ====================

    @Override
    public Event getEventEntity(Long eventId) {
        return repository.findById(eventId)
                .orElseThrow(
                        () -> new NotFoundException(
                                "Event with id " + eventId +
                                        " was not found"
                        )
                );
    }

    @Override
    public boolean existsById(Long eventId) {
        return repository.existsById(eventId);
    }

    @Override
    public List<Event> findAllByIds(
            List<Long> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        return repository.findAllById(eventIds);
    }

    private Event getPublishedEvent(Long eventId) {
        Event event = getEventEntity(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException(
                    "Event with id " + eventId + " not found"
            );
        }

        return event;
    }

    private UserShortDto getUserEntity(Long userId) {
        return userClient.getUser(userId);
    }

    private Category getCategoryEntity(Long categoryId) {
        return categoryService.getCategoryEntity(categoryId);
    }

    private Map<Long, Long> countsFor(
            List<Event> events
    ) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        try {
            return requestClient.getConfirmedCounts(
                    events.stream()
                            .map(Event::getId)
                            .toList()
            );
        } catch (Exception exception) {
            log.warn(
                    "Request service unavailable",
                    exception
            );

            return Map.of();
        }
    }

    /**
     * Получаем рейтинг мероприятий от Analyzer
     * через GetInteractionsCount.
     */
    private Map<Long, Double> ratingsFor(
            List<Event> events
    ) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = events.stream()
                .map(Event::getId)
                .toList();

        return analyzerClient.getInteractionsCount(ids)
                .collect(
                        Collectors.toMap(
                                RecommendedEventProto::getEventId,
                                RecommendedEventProto::getScore,
                                (first, second) -> second
                        )
                );
    }

    private EventFullDto enrichWithRatingAndRequests(
            Event event
    ) {
        return enrichWithRatingAndRequests(
                event,
                countsFor(List.of(event)),
                ratingsFor(List.of(event))
        );
    }

    private EventFullDto enrichWithRatingAndRequests(
            Event event,
            Map<Long, Long> counts,
            Map<Long, Double> ratings
    ) {
        EventFullDto dto = mapper.toFullDto(event);

        dto.setConfirmedRequests(
                counts.getOrDefault(event.getId(), 0L)
        );

        dto.setRating(
                ratings.getOrDefault(event.getId(), 0.0)
        );

        return dto;
    }

    private EventShortDto shortDto(
            Event event,
            Map<Long, Long> counts,
            Map<Long, Double> ratings
    ) {
        EventShortDto dto = mapper.toShortDto(event);

        dto.setConfirmedRequests(
                counts.getOrDefault(event.getId(), 0L)
        );

        dto.setRating(
                ratings.getOrDefault(event.getId(), 0.0)
        );

        return dto;
    }
}
