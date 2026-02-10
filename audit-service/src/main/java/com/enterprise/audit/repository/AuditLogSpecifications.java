package com.enterprise.audit.repository;

import com.cas.common.audit.AuditAction;
import com.cas.common.audit.AuditCategory;
import com.enterprise.audit.entity.AuditLogEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Specifications for dynamic audit log queries
 */
public class AuditLogSpecifications {

    public static Specification<AuditLogEntity> withFilters(
            String actorId,
            AuditCategory category,
            AuditAction action,
            String resourceType,
            String resourceId,
            String serviceName,
            String productCode,
            String correlationId,
            Instant startTime,
            Instant endTime) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actorId != null && !actorId.isBlank()) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (resourceType != null && !resourceType.isBlank()) {
                predicates.add(cb.equal(root.get("resourceType"), resourceType));
            }
            if (resourceId != null && !resourceId.isBlank()) {
                predicates.add(cb.equal(root.get("resourceId"), resourceId));
            }
            if (serviceName != null && !serviceName.isBlank()) {
                predicates.add(cb.equal(root.get("serviceName"), serviceName));
            }
            if (productCode != null && !productCode.isBlank()) {
                predicates.add(cb.equal(root.get("productCode"), productCode));
            }
            if (correlationId != null && !correlationId.isBlank()) {
                predicates.add(cb.equal(root.get("correlationId"), correlationId));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
