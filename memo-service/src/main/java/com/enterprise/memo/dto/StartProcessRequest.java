package com.enterprise.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartProcessRequest {
    private String processTemplateId; // Flowable process definition ID (not a UUID)
    private String businessKey;
    private String title;
    private Map<String, Object> variables;
}
