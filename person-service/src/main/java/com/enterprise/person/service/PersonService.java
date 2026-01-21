package com.enterprise.person.service;

import com.enterprise.person.dto.*;
import com.enterprise.person.entity.Person;
import com.enterprise.person.entity.PersonRelationship;
import com.enterprise.person.entity.PersonRole;
import com.enterprise.person.repository.PersonRelationshipRepository;
import com.enterprise.person.repository.PersonRepository;
import com.enterprise.person.repository.PersonRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonRelationshipRepository relationshipRepository;
    private final PersonRoleRepository roleRepository;

    // === PERSON CRUD ===

    public PersonDTO createPerson(CreatePersonRequest request, UUID createdBy, String createdByName) {
        // Check for duplicates
        if (request.getCitizenshipNumber() != null &&
                personRepository.existsByCitizenshipNumber(request.getCitizenshipNumber())) {
            throw new IllegalArgumentException(
                    "Person with citizenship " + request.getCitizenshipNumber() + " already exists");
        }

        String personCode = generatePersonCode();

        Person person = Person.builder()
                .personCode(personCode)
                .salutation(request.getSalutation())
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .citizenshipNumber(request.getCitizenshipNumber())
                .nationalId(request.getNationalId())
                .passportNumber(request.getPassportNumber())
                .panNumber(request.getPanNumber())
                .photoUrl(request.getPhotoUrl())
                .primaryPhone(request.getPrimaryPhone())
                .secondaryPhone(request.getSecondaryPhone())
                .email(request.getEmail())
                .currentAddress(request.getCurrentAddress())
                .permanentAddress(request.getPermanentAddress())
                .occupationType(request.getOccupationType())
                .employerName(request.getEmployerName())
                .designation(request.getDesignation())
                .monthlyIncome(request.getMonthlyIncome())
                .annualIncome(request.getAnnualIncome())
                .employmentDetails(request.getEmploymentDetails())
                .branchId(request.getBranchId())
                .createdBy(createdBy)
                .createdByName(createdByName)
                .build();

        person = personRepository.save(person);
        log.info("Created person: {} ({})", personCode, person.getFullName());

        return toDTO(person);
    }

    @Transactional(readOnly = true)
    public PersonDTO getPerson(UUID id) {
        return personRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + id));
    }

    @Transactional(readOnly = true)
    public PersonDTO getPersonByCode(String code) {
        return personRepository.findByPersonCode(code)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + code));
    }

    @Transactional(readOnly = true)
    public PersonDTO getPersonByCitizenship(String citizenshipNumber) {
        return personRepository.findByCitizenshipNumber(citizenshipNumber)
                .map(this::toDTO)
                .orElseThrow(
                        () -> new IllegalArgumentException("Person not found with citizenship: " + citizenshipNumber));
    }

    @Transactional(readOnly = true)
    public List<PersonDTO> search(String query) {
        return personRepository.search(query)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PersonDTO updatePerson(UUID id, CreatePersonRequest request) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + id));

        // Update fields
        if (request.getSalutation() != null)
            person.setSalutation(request.getSalutation());
        if (request.getFirstName() != null)
            person.setFirstName(request.getFirstName());
        if (request.getMiddleName() != null)
            person.setMiddleName(request.getMiddleName());
        if (request.getLastName() != null)
            person.setLastName(request.getLastName());
        if (request.getDateOfBirth() != null)
            person.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)
            person.setGender(request.getGender());
        if (request.getPrimaryPhone() != null)
            person.setPrimaryPhone(request.getPrimaryPhone());
        if (request.getSecondaryPhone() != null)
            person.setSecondaryPhone(request.getSecondaryPhone());
        if (request.getEmail() != null)
            person.setEmail(request.getEmail());
        if (request.getCurrentAddress() != null)
            person.setCurrentAddress(request.getCurrentAddress());
        if (request.getPermanentAddress() != null)
            person.setPermanentAddress(request.getPermanentAddress());
        if (request.getOccupationType() != null)
            person.setOccupationType(request.getOccupationType());
        if (request.getEmployerName() != null)
            person.setEmployerName(request.getEmployerName());
        if (request.getDesignation() != null)
            person.setDesignation(request.getDesignation());
        if (request.getMonthlyIncome() != null)
            person.setMonthlyIncome(request.getMonthlyIncome());
        if (request.getAnnualIncome() != null)
            person.setAnnualIncome(request.getAnnualIncome());

        person = personRepository.save(person);
        return toDTO(person);
    }

    public void verifyKyc(UUID personId, UUID verifiedBy) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));

        person.setKycStatus(Person.KycStatus.VERIFIED);
        person.setKycVerifiedAt(LocalDateTime.now());
        person.setKycVerifiedBy(verifiedBy);
        personRepository.save(person);

        log.info("KYC verified for person: {}", person.getPersonCode());
    }

    // === RELATIONSHIPS ===

    public PersonRelationshipDTO addRelationship(UUID personId, UUID relatedPersonId,
            PersonRelationship.RelationshipType type) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));
        Person relatedPerson = personRepository.findById(relatedPersonId)
                .orElseThrow(() -> new IllegalArgumentException("Related person not found: " + relatedPersonId));

        if (relationshipRepository.existsByPersonIdAndRelatedPersonIdAndRelationshipType(
                personId, relatedPersonId, type)) {
            throw new IllegalArgumentException("Relationship already exists");
        }

        PersonRelationship relationship = PersonRelationship.builder()
                .person(person)
                .relatedPerson(relatedPerson)
                .relationshipType(type)
                .build();

        relationship = relationshipRepository.save(relationship);
        log.info("Added relationship: {} <-> {} ({})",
                person.getPersonCode(), relatedPerson.getPersonCode(), type);

        return toRelationshipDTO(relationship);
    }

    @Transactional(readOnly = true)
    public List<PersonRelationshipDTO> getRelationships(UUID personId) {
        return relationshipRepository.findAllRelationships(personId)
                .stream()
                .map(this::toRelationshipDTO)
                .collect(Collectors.toList());
    }

    // === ROLES ===

    public PersonRoleDTO addRole(UUID personId, PersonRole.RoleType roleType,
            String productCode, UUID productId,
            String entityType, UUID entityId,
            UUID createdBy) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));

        if (roleRepository.existsByPersonIdAndEntityTypeAndEntityIdAndRoleType(
                personId, entityType, entityId, roleType)) {
            throw new IllegalArgumentException("Role already exists");
        }

        PersonRole role = PersonRole.builder()
                .person(person)
                .roleType(roleType)
                .productId(productId)
                .productCode(productCode)
                .entityType(entityType)
                .entityId(entityId)
                .startDate(LocalDateTime.now())
                .createdBy(createdBy)
                .build();

        role = roleRepository.save(role);
        log.info("Added role: {} as {} on {} {}",
                person.getPersonCode(), roleType, entityType, entityId);

        return toRoleDTO(role);
    }

    @Transactional(readOnly = true)
    public List<PersonRoleDTO> getRoles(UUID personId) {
        return roleRepository.findByPersonIdAndIsActiveTrue(personId)
                .stream()
                .map(this::toRoleDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PersonRoleDTO> getRolesByProduct(UUID personId, String productCode) {
        return roleRepository.findByPersonIdAndProductCode(personId, productCode)
                .stream()
                .map(this::toRoleDTO)
                .collect(Collectors.toList());
    }

    // === 360° VIEW ===

    @Transactional(readOnly = true)
    public Person360ViewDTO get360View(UUID personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));

        List<PersonRelationshipDTO> relationships = getRelationships(personId);
        List<PersonRoleDTO> roles = getRoles(personId);

        Person360ViewDTO.PersonSummary summary = Person360ViewDTO.PersonSummary.builder()
                .totalRoles(roles.size())
                .activeRoles((int) roles.stream().filter(r -> r.getIsActive()).count())
                .relationships(relationships.size())
                .documentsCount(person.getKycDocuments() != null ? person.getKycDocuments().size() : 0)
                .build();

        return Person360ViewDTO.builder()
                .person(toDTO(person))
                .relationships(relationships)
                .roles(roles)
                .summary(summary)
                .build();
    }

    // === HELPERS ===

    private String generatePersonCode() {
        String prefix = "P-" + LocalDate.now().getYear();
        Integer maxSeq = personRepository.findMaxSequenceByPrefix(prefix);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + "-" + String.format("%05d", nextSeq);
    }

    private PersonDTO toDTO(Person p) {
        return PersonDTO.builder()
                .id(p.getId())
                .personCode(p.getPersonCode())
                .salutation(p.getSalutation())
                .firstName(p.getFirstName())
                .middleName(p.getMiddleName())
                .lastName(p.getLastName())
                .fullName(p.getFullName())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .citizenshipNumber(p.getCitizenshipNumber())
                .nationalId(p.getNationalId())
                .passportNumber(p.getPassportNumber())
                .panNumber(p.getPanNumber())
                .photoUrl(p.getPhotoUrl())
                .primaryPhone(p.getPrimaryPhone())
                .secondaryPhone(p.getSecondaryPhone())
                .email(p.getEmail())
                .currentAddress(p.getCurrentAddress())
                .permanentAddress(p.getPermanentAddress())
                .occupationType(p.getOccupationType())
                .employerName(p.getEmployerName())
                .designation(p.getDesignation())
                .monthlyIncome(p.getMonthlyIncome())
                .annualIncome(p.getAnnualIncome())
                .kycStatus(p.getKycStatus())
                .kycVerifiedAt(p.getKycVerifiedAt())
                .kycDocuments(p.getKycDocuments())
                .isActive(p.getIsActive())
                .branchId(p.getBranchId())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private PersonRelationshipDTO toRelationshipDTO(PersonRelationship r) {
        return PersonRelationshipDTO.builder()
                .id(r.getId())
                .personId(r.getPerson().getId())
                .personName(r.getPerson().getFullName())
                .relatedPersonId(r.getRelatedPerson().getId())
                .relatedPersonName(r.getRelatedPerson().getFullName())
                .relationshipType(r.getRelationshipType())
                .isVerified(r.getIsVerified())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private PersonRoleDTO toRoleDTO(PersonRole r) {
        return PersonRoleDTO.builder()
                .id(r.getId())
                .personId(r.getPerson().getId())
                .personName(r.getPerson().getFullName())
                .roleType(r.getRoleType())
                .productId(r.getProductId())
                .productCode(r.getProductCode())
                .entityType(r.getEntityType())
                .entityId(r.getEntityId())
                .roleDetails(r.getRoleDetails())
                .isActive(r.getIsActive())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
