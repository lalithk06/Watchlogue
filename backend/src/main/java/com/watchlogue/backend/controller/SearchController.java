package com.watchlogue.backend.controller;

import java.util.List;

import com.watchlogue.backend.dto.SearchResultDto;
import com.watchlogue.backend.service.TmdbService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final TmdbService tmdbService;

    public SearchController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @GetMapping
    public List<SearchResultDto> search(
            @RequestParam String query) {
        return tmdbService.searchMulti(query);
    }
}
