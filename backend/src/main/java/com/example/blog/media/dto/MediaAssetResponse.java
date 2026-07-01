package com.example.blog.media.dto;

import java.time.LocalDateTime;

public record MediaAssetResponse(
        Long id,
        String originalName,
        String mediaType,
        String extension,
        long sizeBytes,
        Integer width,
        Integer height,
        String altText,
        String status,
        int referenceCount,
        String url,
        LocalDateTime createdAt
) {
}
