package com.enterprise.form.repository;

import com.enterprise.form.entity.FieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FieldDefinitionRepository extends JpaRepository<FieldDefinition, UUID> {

    List<FieldDefinition> findByFormDefinitionIdOrderByDisplayOrderAsc(UUID formDefinitionId);

    void deleteByFormDefinitionId(UUID formDefinitionId);
}
