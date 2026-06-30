package com.example.blog.site.controller;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class InfrastructureController {

    private final String applicationName;

    public InfrastructureController(
            @Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/status")
    public ResponseEntity<StatusResponse> status() {
        var response = new StatusResponse(applicationName, "ok", Instant.now());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    public record StatusResponse(String application, String status, Instant timestamp) {
    }
}

