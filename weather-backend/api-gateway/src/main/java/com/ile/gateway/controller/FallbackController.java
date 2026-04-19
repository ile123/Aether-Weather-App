package com.ile.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/weather")
    public Mono<ResponseEntity<Map<String, String>>> weatherFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Weather service is currently unavailable")));
    }

    @GetMapping("/alert")
    public Mono<ResponseEntity<Map<String, String>>> alertFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Alert service is currently unavailable")));
    }

    @GetMapping("/preferences")
    public Mono<ResponseEntity<Map<String, String>>> preferencesFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Preferences service is currently unavailable")));
    }
}