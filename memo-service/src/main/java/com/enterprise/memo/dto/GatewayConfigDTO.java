package com.enterprise.memo.dto;

import com.enterprise.memo.entity.WorkflowGatewayConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for gateway configuration API requests/responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayConfigDTO {

        private UUID id;
        private String gatewayId;
        private String gatewayName;
        private String gatewayType; // PARALLEL, INCLUSIVE, EXCLUSIVE
        private String completionMode; // ALL, ANY, N_OF_M
        private Integer minimumRequired;
        private Integer totalIncomingFlows;
        private String description;
        private Boolean cancelRemaining;

        /**
         * Convert entity to DTO.
         */
        public static GatewayConfigDTO fromEntity(WorkflowGatewayConfig entity) {
                return GatewayConfigDTO.builder()
                                .id(entity.getId())
                                .gatewayId(entity.getGatewayId())
                                .gatewayName(entity.getGatewayName())
                                .gatewayType(entity.getGatewayType().name())
                                .completionMode(entity.getCompletionMode().name())
                                .minimumRequired(entity.getMinimumRequired())
                                .totalIncomingFlows(entity.getTotalIncomingFlows())
                                .description(entity.getDescription())
                                .cancelRemaining(entity.getCancelRemaining())
                                .build();
        }

        /**
         * Convert DTO to entity (for saving).
         */
        public WorkflowGatewayConfig toEntity() {
                return WorkflowGatewayConfig.builder()
                                .id(id)
                                .gatewayId(gatewayId)
                                .gatewayName(gatewayName)
                                .gatewayType(gatewayType != null
                                                ? WorkflowGatewayConfig.GatewayType.valueOf(gatewayType)
                                                : WorkflowGatewayConfig.GatewayType.PARALLEL)
                                .completionMode(completionMode != null
                                                ? WorkflowGatewayConfig.CompletionMode.valueOf(completionMode)
                                                : WorkflowGatewayConfig.CompletionMode.ALL)
                                .minimumRequired(minimumRequired != null ? minimumRequired : 1)
                                .totalIncomingFlows(totalIncomingFlows)
                                .description(description)
                                .cancelRemaining(cancelRemaining != null ? cancelRemaining : true)
                                .build();
        }
}
