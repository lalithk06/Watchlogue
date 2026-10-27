package com.watchlogue.backend.dto;

public record SearchResultDto(
        Long tmdbId,
        String title,
        String mediaType,
        Integer year,
        String posterUrl,
        Double tmdbRating
) {
}
