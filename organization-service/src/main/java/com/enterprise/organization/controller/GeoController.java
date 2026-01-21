package com.enterprise.organization.controller;

import com.cas.common.policy.RequiresPolicy;
import com.enterprise.organization.dto.GeoLocationDto;
import com.enterprise.organization.entity.GeoHierarchyType;
import com.enterprise.organization.service.GeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
@RequiresPolicy(resource = "GEO_LOCATION", skip = true)
public class GeoController {

    private final GeoService geoService;

    @GetMapping("/types")
    public ResponseEntity<List<GeoHierarchyType>> getHierarchyTypes(
            @RequestParam(defaultValue = "NP") String country) {
        return ResponseEntity.ok(geoService.getHierarchyTypes(country));
    }

    @GetMapping("/locations")
    public ResponseEntity<List<GeoLocationDto>> getLocations(
            @RequestParam(defaultValue = "NP") String country,
            @RequestParam String type) {
        return ResponseEntity.ok(geoService.getLocationsByType(country, type));
    }

    @GetMapping("/locations/{id}")
    public ResponseEntity<GeoLocationDto> getLocation(@PathVariable UUID id) {
        return ResponseEntity.ok(geoService.getLocation(id));
    }

    @GetMapping("/locations/{id}/children")
    public ResponseEntity<List<GeoLocationDto>> getChildren(@PathVariable UUID id) {
        return ResponseEntity.ok(geoService.getLocationsByParent(id));
    }

    @PostMapping("/locations")
    public ResponseEntity<GeoLocationDto> createLocation(@RequestBody GeoLocationDto.CreateRequest request) {
        return ResponseEntity.ok(geoService.createLocation(request));
    }

    @PutMapping("/locations/{id}")
    public ResponseEntity<GeoLocationDto> updateLocation(
            @PathVariable UUID id,
            @RequestBody GeoLocationDto.UpdateRequest request) {
        return ResponseEntity.ok(geoService.updateLocation(id, request));
    }
}
