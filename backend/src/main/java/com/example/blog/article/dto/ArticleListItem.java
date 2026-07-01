package com.example.blog.article.dto;

import java.time.LocalDateTime;

public record ArticleListItem(
        Long id,
        String title,
        String slug,
        String summary,
        String status,
        String visibility,
        boolean pinned,
        String categoryName,
        int wordCount,
        long viewCount,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        int version
) {
}
