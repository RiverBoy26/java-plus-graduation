package ru.practicum.eventservice.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.dto.EventShortDto;
import ru.practicum.eventservice.dto.EventFullDto;import ru.practicum.eventservice.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.common.config.JacksonConfig.DATE_TIME_FORMAT;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController implements PublicEventControllerApi {
    private final EventService eventService;
    private static final String HEADER = "X-EWM-USER-ID";

    @Override
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {

        log.info("GET /events with params: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, " +
                        "onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        return eventService.getPublicEvents(text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size);
    }

    @Override
    public EventFullDto getEvent(@PathVariable Long id, @RequestHeader(HEADER) long userId) {
        log.info("GET /events/{} by user {}", id, userId);

        return eventService.getPublicEvent(id, userId);
    }

    @Override
    public List<EventShortDto> getRecommendations(@RequestHeader(HEADER) long userId,
                                                  @RequestParam(defaultValue = "10") @Positive int maxResults) {
        log.info(
                "GET /events/recommendations, userId={}",
                userId
        );

        return eventService.getRecommendations(userId, maxResults);
    }

    @Override
    public void likeEvent(@PathVariable Long eventId,
                          @RequestHeader(HEADER) long userId
    ) {
        log.info("PUT /events/{}/like by user {}", eventId, userId);

        eventService.likeEvent(eventId, userId);
    }
}
