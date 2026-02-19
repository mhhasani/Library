package com.library.exception;

import com.library.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("Resource not found", ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex, WebRequest request) {
        log.error("Bad request: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("Bad request", ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        log.error("Unauthorized: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("Unauthorized", ex.getMessage()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("Authentication failed", "Invalid email or password"),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        log.error("Validation error: {}", errors);
        return new ResponseEntity<>(
                ApiResponse.error("Validation failed", errors.toString()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, WebRequest request) {
        log.error("Internal server error", ex);
        return new ResponseEntity<>(
                ApiResponse.error("Internal server error", "An unexpected error occurred"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
