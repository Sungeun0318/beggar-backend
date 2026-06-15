package com.beggar.api.controller;

import com.beggar.api.dto.recommendation.RecommendationInteractionRequest;
import com.beggar.api.dto.recommendation.RecommendationResponse;
import com.beggar.api.security.LoginUser;
import com.beggar.api.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/rooms/{roomNo}/recommend")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public RecommendationResponse recommend(@PathVariable Long roomNo,
                                            @RequestParam(required = false) String tag,
                                            @RequestParam(required = false) String region,
                                            @RequestParam(required = false) Double lat,
                                            @RequestParam(required = false) Double lng,
                                            @RequestParam(defaultValue = "2000") Integer radius,
                                            @RequestParam(defaultValue = "false") boolean strictBudget) {
        return recommendationService.recommend(roomNo, tag, region, lat, lng, radius, strictBudget);
    }

    @PostMapping("/interactions")
    public Map<String, Boolean> recordInteraction(@PathVariable Long roomNo,
                                                  @LoginUser Long userNo,
                                                  @RequestBody RecommendationInteractionRequest request) {
        recommendationService.recordInteraction(roomNo, userNo, request);
        return Map.of("recorded", true);
    }
}
