package com.enterprise.person.repository;

import com.enterprise.person.entity.PersonRole;
import com.enterprise.person.entity.PersonRole.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PersonRoleRepository extends JpaRepository<PersonRole, UUID> {

    List<PersonRole> findByPersonId(UUID personId);

    List<PersonRole> findByPersonIdAndProductId(UUID personId, UUID productId);

    List<PersonRole> findByPersonIdAndProductCode(UUID personId, String productCode);

    List<PersonRole> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    List<PersonRole> findByPersonIdAndRoleType(UUID personId, RoleType roleType);

    List<PersonRole> findByPersonIdAndIsActiveTrue(UUID personId);

    boolean existsByPersonIdAndEntityTypeAndEntityIdAndRoleType(
            UUID personId, String entityType, UUID entityId, RoleType roleType);
}
