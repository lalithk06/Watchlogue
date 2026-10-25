
package com.watchlogue.backend.controller;

import com.watchlogue.backend.dto.TmdbSearchResponse;
import com.watchlogue.backend.service.TmdbService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final TmdbService tmdbService;

    public SearchController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @GetMapping
    public TmdbSearchResponse search(@RequestParam String query) {
        return tmdbService.searchMulti(query);
    }
}
