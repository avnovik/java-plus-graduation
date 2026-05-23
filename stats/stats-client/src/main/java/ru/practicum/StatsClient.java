package ru.practicum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndPointHitDto;
import ru.practicum.dto.StatsParamDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.exception.StatsServerUnavailable;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatsClient {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public StatsClient(DiscoveryClient discoveryClient, RestTemplateBuilder builder) {
        this.discoveryClient = discoveryClient;
        this.statsServiceId = "stats-server";
        this.restTemplate = builder.build();

        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);
        template.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);

        this.retryTemplate = template;
    }

    public void saveHit(EndPointHitDto hitDto) {
        URI uri = makeUri("/hit");
        restTemplate.postForEntity(uri, hitDto, Void.class);
    }

    public List<ViewStatsDto> getStats(StatsParamDto paramDto) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", paramDto.getStartTime().format(FORMATTER))
                .queryParam("end", paramDto.getEndTime().format(FORMATTER));

        if (paramDto.getUris() != null && !paramDto.getUris().isEmpty()) {
            builder.queryParam("uris", paramDto.getUris());
        }

        builder.queryParam("unique", paramDto.isUniques());

        URI uri = makeUri(builder.encode().build().toUriString());

        ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ViewStatsDto>>() {}
        );

        return response.getBody();
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient.getInstances(statsServiceId).getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }
}
