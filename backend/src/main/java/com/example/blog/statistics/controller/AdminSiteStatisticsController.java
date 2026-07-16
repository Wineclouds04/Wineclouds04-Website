package com.example.blog.statistics.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.operation.service.OperationLogService;
import com.example.blog.statistics.dto.PublicSiteStatistics;
import com.example.blog.statistics.dto.SiteStatisticsUpdateRequest;
import com.example.blog.statistics.service.SiteStatisticsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/statistics")
public class AdminSiteStatisticsController {

    private final SiteStatisticsService service;
    private final OperationLogService operationLogs;

    public AdminSiteStatisticsController(SiteStatisticsService service, OperationLogService operationLogs) {
        this.service = service;
        this.operationLogs = operationLogs;
    }

    @GetMapping
    PublicSiteStatistics statistics() {
        return service.adminSnapshot();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    PublicSiteStatistics update(
            @Valid @RequestBody SiteStatisticsUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        PublicSiteStatistics statistics = service.updateTargets(request);
        operationLogs.record(Long.valueOf(jwt.getSubject()), "SITE_STATISTICS", "UPDATE", null, "{}");
        return statistics;
    }
}
