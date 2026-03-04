package com.enterprise.makerchecker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ConfigRequest {

    @NotNull
    private UUID productId;

    @NotBlank
    private String serviceName;

    @NotBlank
    private String endpointPattern;

    @NotBlank
    private String httpMethod;

    private String endpointGroup;

    private String description;

    private Boolean sameMakerCanCheck = false;

    private Boolean enabled = true;

    // Optional SLA config
    private Integer deadlineHours;
    private String escalationRole;
    private Boolean autoExpire = true;

    // Checker user IDs (who can approve)
    private List<UUID> checkerUserIds;
}
