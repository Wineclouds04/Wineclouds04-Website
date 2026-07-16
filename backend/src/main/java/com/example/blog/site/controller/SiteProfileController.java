package com.example.blog.site.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.operation.service.OperationLogService;
import com.example.blog.site.dto.SiteProfileResponse;
import com.example.blog.site.dto.SiteProfileUpdateRequest;
import com.example.blog.site.service.SiteProfileService;

import jakarta.validation.Valid;

@RestController
public class SiteProfileController {

    private final SiteProfileService service;
    private final OperationLogService operationLogs;

    public SiteProfileController(SiteProfileService service, OperationLogService operationLogs) {
        this.service = service;
        this.operationLogs = operationLogs;
    }

    @GetMapping("/api/v1/public/profile")
    SiteProfileResponse publicProfile() {
        return service.publicProfile();
    }

    @GetMapping("/api/v1/admin/profile")
    SiteProfileResponse adminProfile() {
        return service.publicProfile();
    }

    @PutMapping("/api/v1/admin/profile")
    @PreAuthorize("hasRole('ADMIN')")
    SiteProfileResponse update(
            @Valid @RequestBody SiteProfileUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        SiteProfileResponse profile = service.update(request);
        operationLogs.record(Long.valueOf(jwt.getSubject()), "SITE_PROFILE", "UPDATE", null, "{}");
        return profile;
    }
}
