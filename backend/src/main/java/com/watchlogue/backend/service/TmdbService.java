package com.watchlogue.backend.service;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.watchlogue.backend.dto.TmdbSearchResponse;

@Service
public class TmdbService {

    private final RestClient restClient;

    public TmdbService(
            @Value("${tmdb.api.base-url}") String baseUrl,
            @Value("${tmdb.api.access-token}") String accessToken) {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(
                        "Authorization",
                        "Bearer " + accessToken
                )
                .defaultHeader("accept", "application/json")
                .build();
    }

    public TmdbSearchResponse searchMulti(String query) {
    return restClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/search/multi")
                    .queryParam("query", query)
                    .build())
            .retrieve()
            .body(TmdbSearchResponse.class);
    }
}
