package com.enterprise.makerchecker.repository;

import com.enterprise.makerchecker.entity.MakerCheckerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MakerCheckerConfigRepository extends JpaRepository<MakerCheckerConfig, UUID> {

    List<MakerCheckerConfig> findByEnabledTrue();

    List<MakerCheckerConfig> findByProductId(UUID productId);

    Optional<MakerCheckerConfig> findByServiceNameAndEndpointPatternAndHttpMethod(
            String serviceName, String endpointPattern, String httpMethod);

    List<MakerCheckerConfig> findByServiceName(String serviceName);

    boolean existsByServiceNameAndEndpointPatternAndHttpMethod(
            String serviceName, String endpointPattern, String httpMethod);
}
