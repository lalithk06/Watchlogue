package com.watchlogue.backend.dto;

import com.watchlogue.backend.entity.WatchStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateWatchStatusRequest(

        @NotNull(message = "Status is required")
        WatchStatus status

) {
}
