package com.watchlogue.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.watchlogue.backend.dto.AddToLibraryRequest;
import com.watchlogue.backend.dto.LibraryEntryResponse;
import com.watchlogue.backend.dto.SearchResultDto;
import com.watchlogue.backend.dto.UpdateWatchStatusRequest;
import com.watchlogue.backend.entity.LibraryEntry;
import com.watchlogue.backend.entity.Media;
import com.watchlogue.backend.entity.WatchStatus;
import com.watchlogue.backend.repository.LibraryEntryRepository;
import com.watchlogue.backend.repository.MediaRepository;
import com.watchlogue.backend.dto.UpdateLibraryEntryRequest;

@Service
public class LibraryService {

    private final TmdbService tmdbService;
    private final MediaRepository mediaRepository;
    private final LibraryEntryRepository libraryEntryRepository;

    public LibraryService(
            TmdbService tmdbService,
            MediaRepository mediaRepository,
            LibraryEntryRepository libraryEntryRepository) {
        this.tmdbService = tmdbService;
        this.mediaRepository = mediaRepository;
        this.libraryEntryRepository = libraryEntryRepository;
    }

    @Transactional
    public LibraryEntry addToLibrary(AddToLibraryRequest request) {

        SearchResultDto details = tmdbService.getMediaDetails(
                request.tmdbId(),
                request.mediaType()
        );

        Media media = mediaRepository
                .findByTmdbId(details.tmdbId())
                .orElseGet(() -> mediaRepository.save(
                        new Media(
                                details.tmdbId(),
                                details.title(),
                                details.mediaType(),
                                details.year(),
                                details.posterUrl(),
                                details.tmdbRating()
                        )
                ));

        media.setTmdbRating(details.tmdbRating());

        return libraryEntryRepository
                .findByMedia_Id(media.getId())
                .orElseGet(() -> libraryEntryRepository.save(
                        new LibraryEntry(media)
                ));
    }

    @Transactional(readOnly = true)
    public List<LibraryEntryResponse> getLibrary() {

        return libraryEntryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LibraryEntryResponse> getFilteredLibrary(
            WatchStatus status,
            String mediaType,
            Double minTmdbRating,
            Integer minPersonalRating,
            Integer yearFrom,
            Integer yearTo) {

        return libraryEntryRepository.findFilteredLibrary(
                        status,
                        mediaType,
                        minTmdbRating,
                        minPersonalRating,
                        yearFrom,
                        yearTo
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LibraryEntryResponse updateStatus(
            Long libraryEntryId,
            UpdateWatchStatusRequest request) {

        LibraryEntry entry = libraryEntryRepository.findById(libraryEntryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Library entry not found: " + libraryEntryId
                ));

        entry.setStatus(request.status());

        return toResponse(entry);
    }

    @Transactional
    public LibraryEntryResponse updateLibraryEntry(
            Long libraryEntryId,
            UpdateLibraryEntryRequest request) {

        LibraryEntry entry = libraryEntryRepository.findById(libraryEntryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Library entry not found: " + libraryEntryId
                ));

        entry.setRating(request.rating());
        entry.setNotes(request.notes());

        return toResponse(entry);
    }

    private LibraryEntryResponse toResponse(LibraryEntry entry) {

        return new LibraryEntryResponse(
                entry.getId(),
                entry.getMedia().getTmdbId(),
                entry.getMedia().getTitle(),
                entry.getMedia().getMediaType(),
                entry.getMedia().getYear(),
                entry.getMedia().getPosterUrl(),
                entry.getMedia().getTmdbRating(),
                entry.getStatus(),
                entry.getDateAdded(),
                entry.getRating(),
                entry.getNotes(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
