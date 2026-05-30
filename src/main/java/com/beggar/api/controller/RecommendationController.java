package com.beggar.api.controller;

import com.beggar.api.dto.recommendation.RecommendationResponse;
import com.beggar.api.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms/{roomNo}/recommend")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public RecommendationResponse recommend(@PathVariable Long roomNo,
                                            @RequestParam(required = false) String tag,
                                            @RequestParam(required = false) String region) {
        return recommendationService.recommend(roomNo, tag, region);
    }
}
