package com.enterprise.person.dto;

import com.enterprise.person.entity.PersonRole.RoleType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonRoleDTO {
    private UUID id;
    private UUID personId;
    private String personName;
    private RoleType roleType;
    private UUID productId;
    private String productCode;
    private String entityType;
    private UUID entityId;
    private Map<String, Object> roleDetails;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
}
