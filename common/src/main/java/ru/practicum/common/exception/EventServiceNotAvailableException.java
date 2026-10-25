package ru.practicum.common.exception;

public class EventServiceNotAvailableException extends RuntimeException {
    public EventServiceNotAvailableException(Long num, String message) {
        super(message);
    }
    public EventServiceNotAvailableException(Long aLong, Throwable cause) {

    }
    public EventServiceNotAvailableException(String s, Throwable cause) {

    }
    public EventServiceNotAvailableException(Long a, Long b, Throwable cause) {

    }
}