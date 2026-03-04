package com.enterprise.makerchecker.repository;

import com.enterprise.makerchecker.entity.SlaEscalationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlaEscalationConfigRepository extends JpaRepository<SlaEscalationConfig, UUID> {

    Optional<SlaEscalationConfig> findByConfigId(UUID configId);

    void deleteByConfigId(UUID configId);
}
