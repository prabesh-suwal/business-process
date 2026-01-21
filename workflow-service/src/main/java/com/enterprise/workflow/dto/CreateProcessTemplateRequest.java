package com.enterprise.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProcessTemplateRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private String bpmnXml;
}
