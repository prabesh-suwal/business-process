package com.enterprise.memo.dto;

import lombok.Data;
import java.util.Map;

@Data
public class TaskActionRequest {
    private String action; // APPROVE, REJECT, SEND_BACK, FORWARD, CLARIFY
    private String comment;
    private Map<String, Object> variables;
}
