package ru.practicum.requestservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.requestservice.dto.ParticipationRequestDto;
import ru.practicum.requestservice.dto.EventRequestStatusUpdateRequest;
import ru.practicum.requestservice.dto.EventRequestStatusUpdateResult;
import ru.practicum.requestservice.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Validated
public class ParticipationRequestPrivateController implements ParticipationRequestControllerApi {

    private final ParticipationRequestService requestService;

    @Override
    public List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Long userId) {
        return requestService.getUserRequests(userId);
    }

    @Override
    public ParticipationRequestDto addRequest(@PathVariable @Positive Long userId,
                                              @RequestParam @Positive Long eventId) {
        return requestService.addRequest(userId, eventId);
    }

    @Override
    public ParticipationRequestDto cancelRequest(@PathVariable @Positive Long userId,
                                                 @PathVariable @Positive Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId) {
        return requestService.getEventRequests(userId, eventId);
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(@PathVariable @Positive Long userId,
                                                              @PathVariable @Positive Long eventId,
                                                              @RequestBody @Valid EventRequestStatusUpdateRequest updateRequest) {
        return requestService.updateRequestStatus(userId, eventId, updateRequest);
    }
}
