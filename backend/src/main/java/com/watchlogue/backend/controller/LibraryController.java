package com.watchlogue.backend.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.watchlogue.backend.dto.AddToLibraryRequest;
import com.watchlogue.backend.dto.LibraryEntryResponse;
import com.watchlogue.backend.dto.UpdateWatchStatusRequest;
import com.watchlogue.backend.entity.LibraryEntry;
import com.watchlogue.backend.entity.WatchStatus;
import com.watchlogue.backend.service.LibraryService;
import com.watchlogue.backend.dto.UpdateLibraryEntryRequest;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryEntry addToLibrary(
            @Valid @RequestBody AddToLibraryRequest request) {
        return libraryService.addToLibrary(request);
    }

    @GetMapping
    public List<LibraryEntryResponse> getLibrary(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "ALL") String mediaType,
            @RequestParam(defaultValue = "ALL") String genre,
            @RequestParam(required = false) Double minTmdbRating,
            @RequestParam(defaultValue = "0") Integer minPersonalRating,
            @RequestParam(required = false) Double minImdbRating,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo) {

        if (!genre.equalsIgnoreCase("ALL")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Genre filtering is not available until genre metadata is integrated."
            );
        }

        if (minImdbRating != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "IMDb rating filtering is unavailable until an IMDb rating source is integrated."
            );
        }

        if (minTmdbRating != null
                && (minTmdbRating < 0 || minTmdbRating > 10)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum TMDB rating must be between 0 and 10."
            );
        }

        if (minPersonalRating < 0 || minPersonalRating > 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum personal rating must be between 0 and 10."
            );
        }

        if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "yearFrom must not be greater than yearTo."
            );
        }

        WatchStatus parsedStatus = null;

        if (!status.equalsIgnoreCase("ALL")) {
            try {
                parsedStatus = WatchStatus.valueOf(
                        status.toUpperCase(java.util.Locale.ROOT)
                );
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid watch status: " + status
                );
            }
        }

        String parsedMediaType = null;

        if (!mediaType.equalsIgnoreCase("ALL")) {
            parsedMediaType = mediaType.toUpperCase(java.util.Locale.ROOT);

            if (!parsedMediaType.equals("MOVIE")
                    && !parsedMediaType.equals("TV")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Media type must be ALL, MOVIE, or TV."
                );
            }
        }

        Integer parsedMinPersonalRating =
                minPersonalRating == 0 ? null : minPersonalRating;

        return libraryService.getFilteredLibrary(
                parsedStatus,
                parsedMediaType,
                minTmdbRating,
                parsedMinPersonalRating,
                yearFrom,
                yearTo
        );
    }

    @PatchMapping("/{id}/status")
    public LibraryEntryResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWatchStatusRequest request) {

        return libraryService.updateStatus(id, request);
    }

    @PatchMapping("/{id}")
    public LibraryEntryResponse updateLibraryEntry(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLibraryEntryRequest request) {

        return libraryService.updateLibraryEntry(id, request);
    }
}
