package com.watchlogue.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.watchlogue.backend.entity.WatchStatus;

public record LibraryEntryResponse(
        Long id,
        Long tmdbId,
        String title,
        String mediaType,
        Integer year,
        String posterUrl,
        Double tmdbRating,
        WatchStatus status,
        LocalDate dateAdded,
        Integer rating,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
