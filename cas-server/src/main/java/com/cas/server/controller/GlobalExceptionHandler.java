package com.cas.server.controller;

import com.cas.common.dto.ApiResponse;
import com.cas.common.dto.ErrorResponse;
import com.cas.common.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * CAS-specific exception handler.
 * Uses @Order(-1) to take precedence over the common
 * DefaultGlobalExceptionHandler.
 *
 * OAuth2-specific exceptions return ErrorResponse for RFC compliance.
 * Business exceptions return ApiResponse.
 */
@Slf4j
@RestControllerAdvice
@Order(-1)
public class GlobalExceptionHandler {

    @ExceptionHandler(CasException.class)
    public ResponseEntity<ErrorResponse> handleCasException(CasException ex) {
        log.warn("CAS exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .error(ex.getErrorCode())
                .errorDescription(ex.getMessage())
                .status(ex.getHttpStatus())
                .build();
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(401)
                .body(ErrorResponse.oauth2Error("invalid_grant", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(401)
                .body(ErrorResponse.oauth2Error("invalid_token", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientScopeException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientScope(InsufficientScopeException ex) {
        return ResponseEntity.status(403)
                .body(ErrorResponse.oauth2Error("insufficient_scope", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<com.cas.common.dto.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> com.cas.common.dto.FieldError.of(e.getField(), e.getDefaultMessage()))
                .toList();
        log.warn("Validation failed: {} errors", fieldErrors.size());
        return ResponseEntity.badRequest().body(ApiResponse.validationError(fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
