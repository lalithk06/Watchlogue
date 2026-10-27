package com.watchlogue.backend.service;

import java.net.http.HttpClient;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.watchlogue.backend.dto.SearchResultDto;
import com.watchlogue.backend.dto.TmdbSearchResponse;
import com.watchlogue.backend.dto.TmdbSearchResult;

@Service
public class TmdbService {

    private static final String IMAGE_BASE_URL =
            "https://image.tmdb.org/t/p/w342";

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

    public List<SearchResultDto> searchMulti(String query) {
        TmdbSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/multi")
                        .queryParam("query", query)
                        .build())
                .retrieve()
                .body(TmdbSearchResponse.class);

        if (response == null || response.results() == null) {
            return List.of();
        }

        return response.results().stream()
                .filter(this::isMovieOrTv)
                .map(this::toSearchResultDto)
                .toList();
    }

    private boolean isMovieOrTv(TmdbSearchResult result) {
        return result.mediaType() != null
                && (
                    result.mediaType().equals("movie")
                    || result.mediaType().equals("tv")
                );
    }

    private SearchResultDto toSearchResultDto(
            TmdbSearchResult result) {

        String date = result.displayDate();
        Integer year = null;

        if (date != null && date.length() >= 4) {
            try {
                year = Integer.parseInt(date.substring(0, 4));
            } catch (NumberFormatException ignored) {
                // Leave year null if the date is invalid.
            }
        }

        String posterUrl = result.posterPath();

        if (posterUrl != null && !posterUrl.isBlank()) {
            posterUrl = IMAGE_BASE_URL + posterUrl;
        } else {
            posterUrl = null;
        }

        return new SearchResultDto(
                result.id(),
                result.displayTitle(),
                result.mediaType().equals("movie") ? "MOVIE" : "TV",
                year,
                posterUrl,
                result.voteAverage()
        );
    }

    public SearchResultDto getMediaDetails(Long tmdbId, String mediaType) {

        String endpoint = mediaType.equals("MOVIE")
                ? "/movie/" + tmdbId
                : "/tv/" + tmdbId;

        TmdbSearchResult result = restClient.get()
                .uri(endpoint)
                .retrieve()
                .body(TmdbSearchResult.class);

        if (result == null) {
            throw new IllegalStateException(
                    "TMDB returned no details for ID " + tmdbId
            );
        }

        String date = result.displayDate();
        Integer year = null;

        if (date != null && date.length() >= 4) {
            try {
                year = Integer.parseInt(date.substring(0, 4));
            } catch (NumberFormatException ignored) {
                // Keep year null if the date is invalid.
            }
        }

        String posterUrl = result.posterPath();

        if (posterUrl != null && !posterUrl.isBlank()) {
            posterUrl = IMAGE_BASE_URL + posterUrl;
        } else {
            posterUrl = null;
        }

        return new SearchResultDto(
                result.id(),
                result.displayTitle(),
                mediaType,
                year,
                posterUrl,
                result.voteAverage()
        );
    }
}
