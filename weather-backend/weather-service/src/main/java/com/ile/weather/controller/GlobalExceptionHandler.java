package com.ile.weather.controller;

import dto.ErrorItem;
import dto.ErrorResponse;
import exception.ExternalApiException;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(
            ValidationException ex, ServerWebExchange exchange) {
        var correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Validation error: {}", correlationId, ex.getMessage());

        var response = ErrorResponse.builder()
                .type(ErrorResponse.TYPE_VALIDATION)
                .title("Validation Error")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ex.getMessage())
                .instance(URI.create(exchange.getRequest().getPath().toString()))
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBindException(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        var correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Bind error: {}", correlationId, ex.getMessage());

        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorItem(
                        fe.getCode(),
                        fe.getDefaultMessage(),
                        fe.getField(),
                        null,
                        null
                ))
                .toList();

        var response = ErrorResponse.builder()
                .type(ErrorResponse.TYPE_VALIDATION)
                .title("Validation Error")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("One or more fields are invalid")
                .instance(URI.create(exchange.getRequest().getPath().toString()))
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .errors(errors)
                .build();

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(
            ResourceNotFoundException ex, ServerWebExchange exchange) {
        var correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Not found: {}", correlationId, ex.getMessage());

        var response = ErrorResponse.builder()
                .type(ErrorResponse.TYPE_NOT_FOUND)
                .title("Resource Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .detail(ex.getMessage())
                .instance(URI.create(exchange.getRequest().getPath().toString()))
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response));
    }

    @ExceptionHandler(ExternalApiException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleExternalApi(
            ExternalApiException ex, ServerWebExchange exchange) {
        var correlationId = UUID.randomUUID().toString();
        log.error("[{}] External API error: {}", correlationId, ex.getMessage());

        var response = ErrorResponse.builder()
                .type(ErrorResponse.TYPE_EXTERNAL_API)
                .title("External API Error")
                .status(HttpStatus.BAD_GATEWAY.value())
                .detail(ex.getMessage())
                .instance(URI.create(exchange.getRequest().getPath().toString()))
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(
            Exception ex, ServerWebExchange exchange) {
        var correlationId = UUID.randomUUID().toString();
        log.error("[{}] Unexpected error: {}", correlationId, ex.getMessage(), ex);

        var response = ErrorResponse.builder()
                .type(ErrorResponse.TYPE_INTERNAL)
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected error occurred")
                .instance(URI.create(exchange.getRequest().getPath().toString()))
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response));
    }
}