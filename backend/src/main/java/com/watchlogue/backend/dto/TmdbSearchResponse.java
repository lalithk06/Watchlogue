package com.watchlogue.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbSearchResponse(
        Integer page,

        List<TmdbSearchResult> results,

        @JsonProperty("total_pages")
        Integer totalPages,

        @JsonProperty("total_results")
        Integer totalResults
) {
}
