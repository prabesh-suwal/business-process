package com.enterprise.person.repository;

import com.enterprise.person.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {

    Optional<Person> findByPersonCode(String personCode);

    Optional<Person> findByCitizenshipNumber(String citizenshipNumber);

    Optional<Person> findByPrimaryPhone(String primaryPhone);

    Optional<Person> findByEmail(String email);

    boolean existsByCitizenshipNumber(String citizenshipNumber);

    boolean existsByPrimaryPhone(String primaryPhone);

    boolean existsByEmail(String email);

    @Query("SELECT p FROM Person p WHERE " +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "p.primaryPhone LIKE CONCAT('%', :query, '%') OR " +
            "p.citizenshipNumber LIKE CONCAT('%', :query, '%')")
    List<Person> search(@Param("query") String query);

    List<Person> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    @Query("SELECT MAX(CAST(SUBSTRING(p.personCode, LENGTH(:prefix) + 2) AS int)) " +
            "FROM Person p WHERE p.personCode LIKE CONCAT(:prefix, '-%')")
    Integer findMaxSequenceByPrefix(@Param("prefix") String prefix);
}
