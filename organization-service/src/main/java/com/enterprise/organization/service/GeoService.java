package com.enterprise.organization.service;

import com.enterprise.organization.dto.GeoLocationDto;
import com.enterprise.organization.entity.GeoHierarchyType;
import com.enterprise.organization.entity.GeoLocation;
import com.enterprise.organization.repository.GeoHierarchyTypeRepository;
import com.enterprise.organization.repository.GeoLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoService {

    private final GeoHierarchyTypeRepository typeRepository;
    private final GeoLocationRepository locationRepository;

    // ==================== HIERARCHY TYPES ====================

    public List<GeoHierarchyType> getHierarchyTypes(String countryCode) {
        return typeRepository.findByCountryCodeOrderByLevel(countryCode);
    }

    // ==================== LOCATIONS ====================

    public List<GeoLocationDto> getLocationsByType(String countryCode, String typeCode) {
        return locationRepository.findByTypeCodeAndCountry(typeCode, countryCode)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<GeoLocationDto> getLocationsByParent(UUID parentId) {
        return locationRepository.findByParentIdAndStatusOrderByName(parentId, GeoLocation.Status.ACTIVE)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public GeoLocationDto getLocation(UUID id) {
        return locationRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));
    }

    @Transactional
    public GeoLocationDto createLocation(GeoLocationDto.CreateRequest request) {
        GeoHierarchyType type = typeRepository.findByCountryCodeAndCode(request.getCountryCode(), request.getTypeCode())
                .orElseThrow(() -> new RuntimeException("Hierarchy type not found: " + request.getTypeCode()));

        GeoLocation parent = request.getParentId() != null
                ? locationRepository.findById(request.getParentId()).orElse(null)
                : null;

        String fullPath = buildFullPath(parent, request.getCode());

        GeoLocation location = GeoLocation.builder()
                .countryCode(request.getCountryCode())
                .code(request.getCode())
                .name(request.getName())
                .localName(request.getLocalName())
                .type(type)
                .parent(parent)
                .fullPath(fullPath)
                .status(GeoLocation.Status.ACTIVE)
                .build();

        location = locationRepository.save(location);
        log.info("Created geo location: {} ({})", location.getName(), location.getCode());
        return toDto(location);
    }

    @Transactional
    public GeoLocationDto updateLocation(UUID id, GeoLocationDto.UpdateRequest request) {
        GeoLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));

        if (request.getName() != null)
            location.setName(request.getName());
        if (request.getLocalName() != null)
            location.setLocalName(request.getLocalName());
        if (request.getStatus() != null)
            location.setStatus(GeoLocation.Status.valueOf(request.getStatus()));

        location = locationRepository.save(location);
        log.info("Updated geo location: {}", location.getCode());
        return toDto(location);
    }

    private String buildFullPath(GeoLocation parent, String code) {
        if (parent == null)
            return "/" + code;
        return parent.getFullPath() + "/" + code;
    }

    private GeoLocationDto toDto(GeoLocation loc) {
        return GeoLocationDto.builder()
                .id(loc.getId())
                .countryCode(loc.getCountryCode())
                .code(loc.getCode())
                .name(loc.getName())
                .localName(loc.getLocalName())
                .typeCode(loc.getType().getCode())
                .typeName(loc.getType().getName())
                .parentId(loc.getParent() != null ? loc.getParent().getId() : null)
                .parentName(loc.getParent() != null ? loc.getParent().getName() : null)
                .fullPath(loc.getFullPath())
                .status(loc.getStatus().name())
                .build();
    }
}
