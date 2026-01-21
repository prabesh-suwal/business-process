package com.enterprise.organization.repository;

import com.enterprise.organization.entity.GeoHierarchyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeoHierarchyTypeRepository extends JpaRepository<GeoHierarchyType, UUID> {
    List<GeoHierarchyType> findByCountryCodeOrderByLevel(String countryCode);

    Optional<GeoHierarchyType> findByCountryCodeAndCode(String countryCode, String code);

    Optional<GeoHierarchyType> findByCountryCodeAndLevel(String countryCode, Integer level);
}
