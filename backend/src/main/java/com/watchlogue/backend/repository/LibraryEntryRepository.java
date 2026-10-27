package com.watchlogue.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.watchlogue.backend.entity.LibraryEntry;
import com.watchlogue.backend.entity.WatchStatus;

public interface LibraryEntryRepository
        extends JpaRepository<LibraryEntry, Long> {

    List<LibraryEntry> findByStatus(WatchStatus status);

    Optional<LibraryEntry> findByMedia_Id(Long mediaId);

    boolean existsByMedia_Id(Long mediaId);

    @Query("""
            SELECT entry
            FROM LibraryEntry entry
            JOIN entry.media media
            WHERE (:status IS NULL OR entry.status = :status)
              AND (:mediaType IS NULL OR media.mediaType = :mediaType)
              AND (:minTmdbRating IS NULL
                   OR media.tmdbRating >= :minTmdbRating)
              AND (:minPersonalRating IS NULL
                   OR entry.rating >= :minPersonalRating)
              AND (:yearFrom IS NULL OR media.year >= :yearFrom)
              AND (:yearTo IS NULL OR media.year <= :yearTo)
            ORDER BY entry.dateAdded DESC
            """)
    List<LibraryEntry> findFilteredLibrary(
            @Param("status") WatchStatus status,
            @Param("mediaType") String mediaType,
            @Param("minTmdbRating") Double minTmdbRating,
            @Param("minPersonalRating") Integer minPersonalRating,
            @Param("yearFrom") Integer yearFrom,
            @Param("yearTo") Integer yearTo
    );
}
