package com.example.blog.article.model;

import java.time.LocalDateTime;

public record ArticleRecord(
        Long id,
        String title,
        String slug,
        String summary,
        String contentMarkdown,
        String contentHtml,
        Long categoryId,
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
