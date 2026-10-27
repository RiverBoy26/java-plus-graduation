package ru.practicum.eventservice.feign;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.common.exception.ConflictException;
import ru.practicum.common.exception.NotFoundException;

@Configuration
public class FeignErrorConfiguration {
    @Bean
    public ErrorDecoder errorDecoder() {
        ErrorDecoder delegate = new ErrorDecoder.Default();
        return (method, response) -> {
            if (response.status() == 404) return new NotFoundException("Related resource not found: " + method);
            if (response.status() == 409) return new ConflictException("Related resource in conflict: " + method);
            return delegate.decode(method, response);
        };
    }
}
