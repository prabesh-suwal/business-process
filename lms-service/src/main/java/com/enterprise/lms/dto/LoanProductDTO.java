package com.enterprise.lms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProductDTO {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal interestRate;
    private Integer minTenure;
    private Integer maxTenure;
    private BigDecimal processingFeePercent;
    private UUID workflowTemplateId;
    private UUID applicationFormId;
    private Boolean active;
    private Map<String, Object> config;
    private LocalDateTime createdAt;
}
