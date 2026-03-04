package com.enterprise.makerchecker.repository;

import com.enterprise.makerchecker.entity.ConfigChecker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConfigCheckerRepository extends JpaRepository<ConfigChecker, ConfigChecker.ConfigCheckerId> {

    List<ConfigChecker> findByConfigId(UUID configId);

    void deleteByConfigId(UUID configId);
}
