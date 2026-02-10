package com.enterprise.audit.mapper;

import com.enterprise.audit.dto.AuditLogResponse;
import com.enterprise.audit.entity.AuditLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "actorRoles", expression = "java(entity.getActorRolesList())")
    @Mapping(target = "integrityVerified", expression = "java(entity.verifyIntegrity())")
    AuditLogResponse toResponse(AuditLogEntity entity);
}
