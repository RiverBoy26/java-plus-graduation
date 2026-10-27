package ru.practicum.eventservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.common.dto.UserShortDto;
import ru.practicum.eventservice.dto.*;
import ru.practicum.eventservice.feign.UserClient;
import ru.practicum.eventservice.model.Event;
import ru.practicum.eventservice.mapper.EventMapper;
import ru.practicum.eventservice.repository.EventRepository;
import ru.practicum.eventservice.repository.EventRepositoryCustom;
import ru.practicum.eventservice.category.service.CategoryService;
import ru.practicum.eventservice.utils.EventUpdateUtils;
import ru.practicum.eventservice.feign.RequestClient;
import ru.practicum.statistics.client.controller.StatsClient;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.common.entity.Category;
import ru.practicum.common.enums.AdminStateAction;
import ru.practicum.common.enums.EventState;
import ru.practicum.common.enums.SortType;
import ru.practicum.common.exception.BadRequestException;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;
import ru.practicum.common.exception.ValidationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import static ru.practicum.common.config.JacksonConfig.DATE_TIME_FORMAT;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository repository;
    private final EventMapper mapper;
    private final StatsClient statsClient;
    private final UserClient userClient;
    private final CategoryService categoryService;
    private final EventRepositoryCustom eventRepositoryCustom;
    private final RequestClient requestClient;

    private static final String APP_NAME = "ewm-service";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    @Override
    public List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        boolean findWithConfirmedRequests = false;

        if (users != null && users.contains(0L)) {
            findWithConfirmedRequests = true;
            users = null;
        }

        if (categories != null && categories.contains(0L)) {
            categories = null;
        }

        log.info("getAdminEvents: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}, from={}, size={}",
                users, states, categories, rangeStart, rangeEnd, from, size);

        Pageable pageable = PageRequest.of(from / size, size);

        Page<Event> page = eventRepositoryCustom.findAllByAdminFilters(
                users,
                states,
                categories,
                rangeStart,
                rangeEnd,
                pageable
        );

        if (page == null || page.getContent() == null || page.getContent().isEmpty()) {
            return List.of();
        }

        List<Event> events = page.getContent();
        Map<Long, Long> counts = countsFor(events);
        if (findWithConfirmedRequests) {
            events = events.stream().filter(e -> counts.getOrDefault(e.getId(), 0L) > 0).toList();
        }
        Map<Long, Long> views = viewsFor(events);
        return events.stream().map(e -> enrichWithViewsAndRequests(e, counts, views)).toList();
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request) {
        log.info("Updating event by admin: eventId={}, request={}", eventId, request);

        Event event = getEventEntity(eventId);

        if (request.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in PENDING state");
            }

            if (request.getEventDate() != null &&
                    request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Event date must be at least 1 hour from now");
            }
        }

        if (request.getStateAction() == AdminStateAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject published event");
            }
        }

        if (request.getParticipantLimit() != null && request.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant limit must be positive or zero");
        }

        Category category = null;
        if (request.getCategory() != null) {
            category = getCategoryEntity(request.getCategory());
        }

        EventUpdateUtils.updateFromAdmin(event, request, category);

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case PUBLISH_EVENT:
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        event = repository.save(event);
        return enrichWithViewsAndRequests(event);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        log.info("Getting events for user: userId={}", userId);

        getUserEntity(userId);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> page = repository.findByInitiatorId(userId, pageable);

        List<Event> events = page.getContent();
        Map<Long, Long> counts = countsFor(events);
        Map<Long, Long> views = viewsFor(events);
        return events.stream().map(e -> shortDto(e, counts, views)).toList();
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        log.info("Creating event for user: userId={}, dto={}", userId, dto);

        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new ValidationException("description must not be blank");
        }

        if (dto.getAnnotation() == null || dto.getAnnotation().isBlank()) {
            throw new ValidationException("annotation must not be blank");
        }

        if (dto.getParticipantLimit() != null && dto.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant limit cannot be negative");
        }

        UserShortDto user = getUserEntity(userId);

        Category category = getCategoryEntity(dto.getCategory());

        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }

        Event event = mapper.toEntity(dto, category);
        event.setInitiatorId(user.getId());
        event.setInitiatorName(user.getName());
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());
        event.setPaid(dto.getPaid() != null ? dto.getPaid() : false);
        event.setParticipantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0);
        event.setRequestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true);

        event = repository.save(event);
        log.info("Created event with id: {}", event.getId());

        EventFullDto result = mapper.toFullDto(event);
        result.setConfirmedRequests(0L);
        result.setViews(0L);
        return result;
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("Getting event for user: userId={}, eventId={}", userId, eventId);

        getUserEntity(userId);

        Event event = getEventEntity(eventId);

        if (event.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant limit not be negative");
        }

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Event with id " + eventId + " not found for user " + userId);
        }

        return enrichWithViewsAndRequests(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Updating event by user: userId={}, eventId={}, request={}", userId, eventId, request);

        getUserEntity(userId);

        Event event = getEventEntity(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Event with id " + eventId + " not found for user " + userId);
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (request.getEventDate() != null &&
                request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }

        if (request.getParticipantLimit() != null && request.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant Limit must be positive or zero");
        }

        Category category = null;

        if (request.getCategory() != null) {
            category = getCategoryEntity(request.getCategory());
        }

        EventUpdateUtils.updateFromUser(event, request, category);

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
                default -> throw new BadRequestException("Unknown state action: " + request.getStateAction());
            }
        }

        event = repository.save(event);
        log.info("Updated event with id: {}", eventId);

        return enrichWithViewsAndRequests(event);
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, int from, int size) {
        log.info("Getting public events with filters: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, " +
                        "onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        if (rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Range end do not to be after range start");
        }

        if (text == null) {
            text = "";
        }

        Pageable pageable;
        if (sort != null && sort.equalsIgnoreCase(SortType.VIEWS.name())) {
            pageable = PageRequest.of(from / size, size);
        } else {
            pageable = PageRequest.of(from / size, size, Sort.by("eventDate").ascending());
        }

        Page<Event> page = eventRepositoryCustom.findPublishedEvents(text, categories, paid, rangeStart, rangeEnd, pageable);
        List<Event> events = page.getContent();

        Map<Long, Long> confirmed = countsFor(events);
        if (onlyAvailable != null && onlyAvailable) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 ||
                            confirmed.getOrDefault(e.getId(), 0L) < e.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        Map<Long, Long> views = viewsFor(events);
        if (sort != null && sort.equalsIgnoreCase(SortType.VIEWS.name())) {
            events = events.stream().sorted(Comparator.comparingLong(
                    (Event e) -> views.getOrDefault(e.getId(), 0L)).reversed()).toList();
        }

        List<EventShortDto> dtos = events.stream()
                .map(e -> shortDto(e, confirmed, views)).toList();

        statsClient.sendHit(new EndpointHit(APP_NAME, "/events", "0.0.0.0", LocalDateTime.now()));

        return dtos;
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId) {
        log.info("Getting public event: eventId={}", eventId);

        Event event = getEventEntity(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id " + eventId + " not found");
        }

        statsClient.sendHit(new EndpointHit(
                APP_NAME,
                "/events/" + eventId,
                "0.0.0.0",
                LocalDateTime.now()));

        return enrichWithViewsAndRequests(event);
    }

    @Override
    public Event getEventEntity(Long eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " was not found"));
    }

    @Override
    public boolean existsById(Long eventId) {
        return repository.existsById(eventId);
    }

    @Override
    public List<Event> findAllByIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(eventIds);
    }

    private EventFullDto enrichWithViewsAndRequests(Event event) {
        if (event == null) {
            return null;
        }
        return enrichWithViewsAndRequests(event, countsFor(List.of(event)), viewsFor(List.of(event)));
    }

    private EventShortDto shortDto(Event event, Map<Long, Long> counts, Map<Long, Long> views) {
        EventShortDto dto = mapper.toShortDto(event);
        dto.setConfirmedRequests(counts.getOrDefault(event.getId(), 0L));
        dto.setViews(views.getOrDefault(event.getId(), 0L));
        return dto;
    }

    private Map<Long, Long> viewsFor(List<Event> events) {
        if (events.isEmpty()) return Map.of();
        try {
            LocalDateTime start = events.stream()
                    .map(e -> e.getPublishedOn() != null ? e.getPublishedOn() : e.getCreatedOn())
                    .filter(java.util.Objects::nonNull)
                    .min(LocalDateTime::compareTo).orElse(LocalDateTime.now().minusDays(1));
            List<String> uris = events.stream().map(e -> "/events/" + e.getId()).toList();
            List<ViewStats> stats = statsClient.getStats(start.format(FORMATTER),
                    LocalDateTime.now().format(FORMATTER), uris, true);
            Map<Long, Long> result = new HashMap<>();
            for (ViewStats stat : stats) {
                String uri = stat.getUri();
                if (uri.startsWith("/events/")) {
                    try {
                        result.put(Long.valueOf(uri.substring("/events/".length())), stat.getHits());
                    } catch (NumberFormatException ignored) {
                        // Ignore unknown stats paths.
                    }
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("Stats unavailable for events batch", ex);
            return Map.of();
        }
    }

    private UserShortDto getUserEntity(Long userId) {
        return userClient.getUser(userId);
    }

    private Map<Long, Long> countsFor(List<Event> events) {
        if (events.isEmpty()) return Map.of();
        try {
            return requestClient.getConfirmedCounts(events.stream().map(Event::getId).toList());
        } catch (Exception ex) {
            log.warn("Request service unavailable, using zero for read-side counts", ex);
            return Map.of();
        }
    }

    private EventFullDto enrichWithViewsAndRequests(Event event, Map<Long, Long> counts,
                                                    Map<Long, Long> views) {
        EventFullDto dto = mapper.toFullDto(event);
        dto.setConfirmedRequests(counts.getOrDefault(event.getId(), 0L));
        dto.setViews(views.getOrDefault(event.getId(), 0L));
        return dto;
    }

    private Category getCategoryEntity(Long categoryId) {
        return categoryService.getCategoryEntity(categoryId);
    }

}
