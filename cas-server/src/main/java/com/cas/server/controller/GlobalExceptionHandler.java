package com.cas.server.controller;

import com.cas.common.dto.ErrorResponse;
import com.cas.common.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CasException.class)
    public ResponseEntity<ErrorResponse> handleCasException(CasException ex) {
        log.warn("CAS exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .error(ex.getErrorCode())
                .errorDescription(ex.getMessage())
                .status(ex.getHttpStatus())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        ErrorResponse response = ErrorResponse.oauth2Error("invalid_grant", ex.getMessage());
        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        ErrorResponse response = ErrorResponse.oauth2Error("invalid_token", ex.getMessage());
        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(InsufficientScopeException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientScope(InsufficientScopeException ex) {
        ErrorResponse response = ErrorResponse.oauth2Error("insufficient_scope", ex.getMessage());
        return ResponseEntity.status(403).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse response = ErrorResponse.builder()
                .error("validation_error")
                .errorDescription("Invalid request parameters")
                .status(400)
                .fieldErrors(fieldErrors)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        ErrorResponse response = ErrorResponse.builder()
                .error("server_error")
                .errorDescription("An unexpected error occurred")
                .status(500)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
