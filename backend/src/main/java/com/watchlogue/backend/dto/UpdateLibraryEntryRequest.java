package com.watchlogue.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateLibraryEntryRequest(

        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 10, message = "Rating must not exceed 10")
        Integer rating,

        @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
        String notes

) {
}
