package com.enterprise.workflow.dto;

import lombok.Data;

@Data
public class SendBackRequest {
    private String targetActivityId;
    private String reason;
}
