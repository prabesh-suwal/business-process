package com.enterprise.person.dto;

import com.enterprise.person.entity.PersonRelationship.RelationshipType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonRelationshipDTO {
    private UUID id;
    private UUID personId;
    private String personName;
    private UUID relatedPersonId;
    private String relatedPersonName;
    private RelationshipType relationshipType;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}
