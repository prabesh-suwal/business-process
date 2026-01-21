package com.enterprise.person.controller;

import com.enterprise.person.dto.*;
import com.enterprise.person.entity.PersonRelationship;
import com.enterprise.person.entity.PersonRole;
import com.enterprise.person.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    // === PERSON CRUD ===

    @PostMapping
    public ResponseEntity<PersonDTO> createPerson(
            @Valid @RequestBody CreatePersonRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        PersonDTO person = personService.createPerson(request, userId, userName);
        return ResponseEntity.ok(person);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonDTO> getPerson(@PathVariable UUID id) {
        return ResponseEntity.ok(personService.getPerson(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<PersonDTO> getPersonByCode(@PathVariable String code) {
        return ResponseEntity.ok(personService.getPersonByCode(code));
    }

    @GetMapping("/by-identifier")
    public ResponseEntity<PersonDTO> getByIdentifier(
            @RequestParam String type,
            @RequestParam String value) {
        if ("CITIZENSHIP".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(personService.getPersonByCitizenship(value));
        }
        throw new IllegalArgumentException("Unsupported identifier type: " + type);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PersonDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(personService.search(q));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonDTO> updatePerson(
            @PathVariable UUID id,
            @RequestBody CreatePersonRequest request) {
        return ResponseEntity.ok(personService.updatePerson(id, request));
    }

    @PostMapping("/{id}/verify-kyc")
    public ResponseEntity<Void> verifyKyc(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID verifiedBy) {
        personService.verifyKyc(id, verifiedBy);
        return ResponseEntity.ok().build();
    }

    // === RELATIONSHIPS ===

    @PostMapping("/{id}/relationships")
    public ResponseEntity<PersonRelationshipDTO> addRelationship(
            @PathVariable UUID id,
            @RequestBody AddRelationshipRequest request) {
        PersonRelationshipDTO relationship = personService.addRelationship(
                id,
                request.getRelatedPersonId(),
                request.getRelationshipType());
        return ResponseEntity.ok(relationship);
    }

    @GetMapping("/{id}/relationships")
    public ResponseEntity<List<PersonRelationshipDTO>> getRelationships(@PathVariable UUID id) {
        return ResponseEntity.ok(personService.getRelationships(id));
    }

    // === ROLES ===

    @PostMapping("/{id}/roles")
    public ResponseEntity<PersonRoleDTO> addRole(
            @PathVariable UUID id,
            @RequestBody AddRoleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        PersonRoleDTO role = personService.addRole(
                id,
                request.getRoleType(),
                request.getProductCode(),
                request.getProductId(),
                request.getEntityType(),
                request.getEntityId(),
                userId);
        return ResponseEntity.ok(role);
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<List<PersonRoleDTO>> getRoles(
            @PathVariable UUID id,
            @RequestParam(required = false) String product) {
        if (product != null) {
            return ResponseEntity.ok(personService.getRolesByProduct(id, product));
        }
        return ResponseEntity.ok(personService.getRoles(id));
    }

    // === 360° VIEW ===

    @GetMapping("/{id}/360-view")
    public ResponseEntity<Person360ViewDTO> get360View(@PathVariable UUID id) {
        return ResponseEntity.ok(personService.get360View(id));
    }

    // === REQUEST DTOs ===

    @lombok.Data
    public static class AddRelationshipRequest {
        private UUID relatedPersonId;
        private PersonRelationship.RelationshipType relationshipType;
    }

    @lombok.Data
    public static class AddRoleRequest {
        private PersonRole.RoleType roleType;
        private String productCode;
        private UUID productId;
        private String entityType;
        private UUID entityId;
        private Map<String, Object> roleDetails;
    }
}
