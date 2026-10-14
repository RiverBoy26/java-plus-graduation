package ru.practicum.statistics.client.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.statistics.client.controller.api.StatsClientApi;
import ru.practicum.statistics.client.exception.StatsServerUnavailable;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient implements StatsClientApi {

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate = createRetryTemplate();

    @Value("${stats-server.service-id:stats-server}")
    private String statsServiceId;

    private static RetryTemplate createRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);
        template.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    private ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances =
                    discoveryClient.getInstances(statsServiceId);

            if (instances == null || instances.isEmpty()) {
                throw new IllegalStateException(
                        "Нет зарегистрированных экземпляров сервиса: "
                                + statsServiceId
                );
            }

            return instances.get(0);
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: "
                            + statsServiceId,
                    exception
            );
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance =
                retryTemplate.execute(context -> getInstance());

        return UriComponentsBuilder
                .fromUri(instance.getUri())
                .path(path)
                .build()
                .toUri();
    }

    @Override
    public void sendHit(EndpointHit hit) {
        try {
            restTemplate.postForEntity(
                    makeUri("/hit"),
                    hit,
                    Void.class
            );

            log.info("Отправлен хит: {}", hit);
        } catch (Exception e) {
            log.error("Ошибка при отправке хита: {}", e.getMessage());
        }
    }

    @Override
    public List<ViewStats> getStats(
            String start,
            String end,
            List<String> uris,
            Boolean unique
    ) {
        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder
                            .fromUri(makeUri("/stats"))
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .queryParam("uris", String.join(",", uris))
                            .queryParam("unique", unique);

            String url = builder.build().toUriString();

            log.info("Requesting stats: {}", url);

            ResponseEntity<List<ViewStats>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<List<ViewStats>>() {
                            }
                    );

            log.info("Stats response status: {}", response.getStatusCode());
            log.info("Stats response body: {}", response.getBody());

            return response.getBody() != null
                    ? response.getBody()
                    : List.of();

        } catch (Exception e) {
            log.error("Error getting stats: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
