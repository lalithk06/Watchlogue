package com.watchlogue.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbSearchResult(
        Long id,

        @JsonProperty("media_type")
        String mediaType,

        Boolean adult,

        @JsonProperty("poster_path")
        String posterPath,

        @JsonProperty("backdrop_path")
        String backdropPath,

        String overview,

        @JsonProperty("genre_ids")
        List<Integer> genreIds,

        String title,
        String name,

        @JsonProperty("original_title")
        String originalTitle,

        @JsonProperty("original_name")
        String originalName,

        @JsonProperty("release_date")
        String releaseDate,

        @JsonProperty("first_air_date")
        String firstAirDate,

        @JsonProperty("original_language")
        String originalLanguage,

        Double popularity,

        @JsonProperty("vote_average")
        Double voteAverage,

        @JsonProperty("vote_count")
        Integer voteCount
) {
    @JsonProperty("displayTitle")
    public String displayTitle() {
        if (title != null && !title.isBlank()) {
            return title;
        }

        return name;
    }

    @JsonProperty("displayDate")
    public String displayDate() {
        if (releaseDate != null && !releaseDate.isBlank()) {
            return releaseDate;
        }

        return firstAirDate;
    }
}
