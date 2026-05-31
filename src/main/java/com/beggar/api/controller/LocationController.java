package com.beggar.api.controller;

import com.beggar.api.dto.location.LocationSearchResponse;
import com.beggar.api.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/locations/search")
    public List<LocationSearchResponse> search(@RequestParam String query) {
        return locationService.search(query);
    }
}
