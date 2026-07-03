package com.example.blog.media.controller;

import java.time.Duration;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.blog.media.dto.MediaAltTextRequest;
import com.example.blog.media.dto.MediaAssetResponse;
import com.example.blog.media.dto.MediaConfigResponse;
import com.example.blog.media.dto.MediaPageResponse;
import com.example.blog.media.service.MediaService;
import com.example.blog.operation.service.OperationLogService;
import com.example.blog.shared.security.RateLimitService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/media")
public class AdminMediaController {

    private final MediaService mediaService;
    private final OperationLogService operationLogs;
    private final RateLimitService rateLimits;

    public AdminMediaController(
            MediaService mediaService,
            OperationLogService operationLogs,
            RateLimitService rateLimits
    ) {
        this.mediaService = mediaService;
        this.operationLogs = operationLogs;
        this.rateLimits = rateLimits;
    }

    @GetMapping("/config")
    MediaConfigResponse config() {
        return mediaService.config();
    }

    @GetMapping
    MediaPageResponse media(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int pageSize
    ) {
        return mediaService.findPage(page, pageSize);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    MediaAssetResponse upload(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String altText,
            @AuthenticationPrincipal Jwt jwt
    ) {
        rateLimits.enforce(
                "upload",
                jwt.getSubject(),
                20,
                Duration.ofMinutes(1)
        );
        MediaAssetResponse created = mediaService.upload(file, altText, Long.valueOf(jwt.getSubject()));
        operationLogs.record(Long.valueOf(jwt.getSubject()), "MEDIA", "UPLOAD", created.id(), "{}");
        return created;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    MediaAssetResponse update(
            @PathVariable Long id,
            @Valid @RequestBody MediaAltTextRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        MediaAssetResponse updated = mediaService.updateAltText(id, request.altText());
        operationLogs.record(Long.valueOf(jwt.getSubject()), "MEDIA", "UPDATE", id, "{}");
        return updated;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        mediaService.delete(id);
        operationLogs.record(Long.valueOf(jwt.getSubject()), "MEDIA", "DELETE", id, "{}");
        return ResponseEntity.noContent().build();
    }
}
