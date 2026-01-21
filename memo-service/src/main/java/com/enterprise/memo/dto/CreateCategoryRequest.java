package com.enterprise.memo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCategoryRequest {
    @NotBlank
    private String code; // e.g. "FINANCE"
    @NotBlank
    private String name; // e.g. "Finance Department"
    private String description;
    private String accessPolicy;
}
