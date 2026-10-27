package com.watchlogue.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

public record AddToLibraryRequest(

        @NotNull(message = "TMDB ID is required")
        @Positive(message = "TMDB ID must be positive")
        Long tmdbId,

        @NotNull(message = "Media type is required")
        @Pattern(
                regexp = "MOVIE|TV",
                message = "Media type must be MOVIE or TV"
        )
        String mediaType

) {
}
