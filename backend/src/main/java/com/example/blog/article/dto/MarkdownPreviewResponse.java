package com.example.blog.article.dto;

public record MarkdownPreviewResponse(
        String html,
        String plain,
        int wordCount,
        int readingMinutes
) {
}
