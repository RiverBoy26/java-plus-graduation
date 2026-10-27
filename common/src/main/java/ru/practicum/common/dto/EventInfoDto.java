package ru.practicum.common.dto;

import lombok.Data;
import ru.practicum.common.enums.EventState;

@Data
public class EventInfoDto {
    private Long id;
    private Long initiatorId;
    private EventState state;
    private Integer participantLimit;
    private Boolean requestModeration;
}