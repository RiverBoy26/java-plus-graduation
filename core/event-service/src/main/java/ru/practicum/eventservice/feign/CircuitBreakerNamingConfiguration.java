package ru.practicum.eventservice.feign;

import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Bind configured Resilience4j policy to the batched request-count reader. */
@Configuration
public class CircuitBreakerNamingConfiguration {
    @Bean
    public CircuitBreakerNameResolver circuitBreakerNameResolver() {
        return (client, target, method) ->
                method.getDeclaringClass().equals(RequestClient.class)
                        ? "event-requests"
                        : client + "-" + method.getName();
    }
}
