package com.enterprise.form.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResultDTO {

    private boolean valid;
    private List<ValidationError> errors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationError {
        private String fieldKey;
        private String message;
        private String rule;
        private Object value;
    }
}
