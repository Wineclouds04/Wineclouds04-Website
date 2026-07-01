package com.example.blog.media.controller;

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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/media")
public class AdminMediaController {

    private final MediaService mediaService;

    public AdminMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
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
        return mediaService.upload(file, altText, Long.valueOf(jwt.getSubject()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    MediaAssetResponse update(
            @PathVariable Long id,
            @Valid @RequestBody MediaAltTextRequest request
    ) {
        return mediaService.updateAltText(id, request.altText());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        mediaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
