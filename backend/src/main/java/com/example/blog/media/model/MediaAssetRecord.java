package com.example.blog.media.model;

import java.time.LocalDateTime;

public record MediaAssetRecord(
        Long id,
        String objectKey,
        String originalName,
        String mediaType,
        String extension,
        long sizeBytes,
        Integer width,
        Integer height,
        String sha256,
        String altText,
        String status,
        int referenceCount,
        LocalDateTime unreferencedAt,
        int deleteAttempts,
        LocalDateTime nextDeleteRetryAt,
        String lastDeleteError,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
}
