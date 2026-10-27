package ru.practicum.requestservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.common.enums.RequestStatus;
import ru.practicum.requestservice.repository.ParticipationRequestRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/requests")
public class InternalRequestController {

    private final ParticipationRequestRepository requestRepository;

    @PostMapping("/confirmed-counts")
    public Map<Long, Long> getConfirmedCounts(
            @RequestBody List<Long> eventIds) {

        Map<Long, Long> result = new HashMap<>();

        for (Long id : eventIds) {
            result.put(id, 0L);
        }

        if (eventIds.isEmpty()) {
            return result;
        }

        List<ParticipationRequestRepository.ConfirmedCount> counts =
                requestRepository.confirmedCounts(
                        eventIds, RequestStatus.CONFIRMED);

        for (ParticipationRequestRepository.ConfirmedCount count : counts) {
            result.put(count.getEventId(), count.getConfirmed());
        }

        return result;
    }
}