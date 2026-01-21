package com.enterprise.person.repository;

import com.enterprise.person.entity.PersonRelationship;
import com.enterprise.person.entity.PersonRelationship.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRelationshipRepository extends JpaRepository<PersonRelationship, UUID> {

    List<PersonRelationship> findByPersonId(UUID personId);

    List<PersonRelationship> findByRelatedPersonId(UUID relatedPersonId);

    Optional<PersonRelationship> findByPersonIdAndRelatedPersonIdAndRelationshipType(
            UUID personId, UUID relatedPersonId, RelationshipType type);

    @Query("SELECT r FROM PersonRelationship r WHERE r.person.id = :personId OR r.relatedPerson.id = :personId")
    List<PersonRelationship> findAllRelationships(@Param("personId") UUID personId);

    boolean existsByPersonIdAndRelatedPersonIdAndRelationshipType(
            UUID personId, UUID relatedPersonId, RelationshipType type);
}
