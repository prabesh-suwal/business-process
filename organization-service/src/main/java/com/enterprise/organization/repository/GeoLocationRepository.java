package com.enterprise.organization.repository;

import com.enterprise.organization.entity.GeoLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeoLocationRepository extends JpaRepository<GeoLocation, UUID> {
    List<GeoLocation> findByCountryCodeAndStatusOrderByName(String countryCode, GeoLocation.Status status);

    List<GeoLocation> findByTypeIdAndStatusOrderByName(UUID typeId, GeoLocation.Status status);

    List<GeoLocation> findByParentIdAndStatusOrderByName(UUID parentId, GeoLocation.Status status);

    Optional<GeoLocation> findByCountryCodeAndCode(String countryCode, String code);

    @Query("SELECT g FROM GeoLocation g WHERE g.type.code = :typeCode AND g.countryCode = :countryCode AND g.status = 'ACTIVE' ORDER BY g.name")
    List<GeoLocation> findByTypeCodeAndCountry(String typeCode, String countryCode);
}
