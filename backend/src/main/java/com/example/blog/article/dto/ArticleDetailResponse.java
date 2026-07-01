package com.example.blog.article.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleDetailResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String contentMarkdown,
        String contentHtml,
        Long categoryId,
        List<Long> tagIds,
        String status,
        String visibility,
        boolean pinned,
        boolean allowComment,
        int wordCount,
        int readingMinutes,
        long viewCount,
        String metaTitle,
        String metaDescription,
        String canonicalUrl,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int version
) {
}
