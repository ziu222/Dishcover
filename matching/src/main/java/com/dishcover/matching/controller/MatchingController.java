package com.dishcover.matching.controller;

import com.dishcover.common.security.RequiresPlan;
import com.dishcover.matching.dto.MatchingDtos.RecipeMatchResponse;
import com.dishcover.matching.service.MatchingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gợi ý công thức theo nguyên liệu — tính năng PRO (CLAUDE.md mục 8). */
@RestController
@RequestMapping("/matching")
public class MatchingController {

    private final MatchingService service;

    public MatchingController(MatchingService service) {
        this.service = service;
    }

    @RequiresPlan("PRO")
    @GetMapping("/suggestions")
    public List<RecipeMatchResponse> suggestions(@RequestHeader("Authorization") String bearerToken,
                                                  @RequestParam(required = false) Integer topN) {
        return service.suggest(bearerToken, topN);
    }
}
