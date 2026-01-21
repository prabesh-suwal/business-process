package com.enterprise.person.dto;

import lombok.*;

import java.util.List;

/**
 * 360° view of a person - all info in one response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person360ViewDTO {
    private PersonDTO person;
    private List<PersonRelationshipDTO> relationships;
    private List<PersonRoleDTO> roles;
    private PersonSummary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PersonSummary {
        private int totalRoles;
        private int activeRoles;
        private int relationships;
        private int documentsCount;
    }
}
