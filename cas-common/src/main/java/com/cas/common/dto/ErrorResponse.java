package com.cas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String error;
    private String errorDescription;
    private String errorUri;
    private Integer status;
    private String path;
    private Instant timestamp;
    private Map<String, String> fieldErrors;

    public static ErrorResponse of(String error, String description) {
        return ErrorResponse.builder()
                .error(error)
                .errorDescription(description)
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponse oauth2Error(String error, String description) {
        return ErrorResponse.builder()
                .error(error)
                .errorDescription(description)
                .build();
    }
}
