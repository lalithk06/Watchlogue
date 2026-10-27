package com.watchlogue.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.watchlogue.backend.entity.Media;

public interface MediaRepository extends JpaRepository<Media, Long> {

    Optional<Media> findByTmdbId(Long tmdbId);

    boolean existsByTmdbId(Long tmdbId);
}
