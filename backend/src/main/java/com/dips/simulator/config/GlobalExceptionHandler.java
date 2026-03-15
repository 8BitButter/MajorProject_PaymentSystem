package com.dips.simulator.config;

import com.dips.simulator.logging.LogContext;
import com.dips.simulator.service.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, String>> handleDomain(DomainException ex) {
        log.warn("event=domain_error message={}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "error", ex.getMessage(),
                "correlationId", safeCorrelationId()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError err : ex.getBindingResult().getFieldErrors()) {
            fields.put(err.getField(), err.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("error", "validation_failed");
        body.put("fields", fields);
        body.put("correlationId", safeCorrelationId());
        log.warn("event=validation_error fields={}", fields.keySet());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAny(Exception ex) {
        log.error("event=internal_error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "internal_error",
                        "message", ex.getMessage(),
                        "correlationId", safeCorrelationId()
                ));
    }

    private String safeCorrelationId() {
        String correlationId = LogContext.getCorrelationId();
        return correlationId == null ? "na" : correlationId;
    }
}

