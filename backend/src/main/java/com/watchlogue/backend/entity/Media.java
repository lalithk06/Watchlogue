package com.watchlogue.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "media",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_media_tmdb_id",
            columnNames = "tmdb_id"
        )
    }
)
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Column(nullable = false)
    private String title;

    @Column(name = "media_type", nullable = false, length = 10)
    private String mediaType;

    private Integer year;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "tmdb_rating")
    private Double tmdbRating;

    protected Media() {
        // Required by JPA
    }

    public Media(
            Long tmdbId,
            String title,
            String mediaType,
            Integer year,
            String posterUrl,
            Double tmdbRating) {
        this.tmdbId = tmdbId;
        this.title = title;
        this.mediaType = mediaType;
        this.year = year;
        this.posterUrl = posterUrl;
        this.tmdbRating = tmdbRating;
    }

    public Long getId() {
        return id;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public String getTitle() {
        return title;
    }

    public String getMediaType() {
        return mediaType;
    }

    public Integer getYear() {
        return year;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public Double getTmdbRating() {
        return tmdbRating;
    }

    public void setTmdbRating(Double tmdbRating) {
        this.tmdbRating = tmdbRating;
    }
}
