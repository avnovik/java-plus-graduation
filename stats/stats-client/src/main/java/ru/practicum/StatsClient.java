package ru.practicum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndPointHitDto;
import ru.practicum.dto.StatsParamDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatsClient {

    private final RestTemplate restTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public StatsClient(@Value("${stats-server.url:http://stats-server:9090}") String serverUrl, RestTemplateBuilder builder) {
        this.restTemplate = builder
                .rootUri(serverUrl)
                .build();
    }

    public void saveHit(EndPointHitDto hitDto) {
        restTemplate.postForEntity("/hit", hitDto, Void.class);
    }

    public List<ViewStatsDto> getStats(StatsParamDto paramDto) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", paramDto.getStartTime().format(FORMATTER))
                .queryParam("end", paramDto.getEndTime().format(FORMATTER));

        if (paramDto.getUris() != null && !paramDto.getUris().isEmpty()) {
            builder.queryParam("uris", paramDto.getUris());
        }

        builder.queryParam("unique", paramDto.isUniques());

        ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                builder.build().toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ViewStatsDto>>() {}
        );

        return response.getBody();
    }
}
