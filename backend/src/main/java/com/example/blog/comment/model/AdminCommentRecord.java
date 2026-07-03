package com.example.blog.comment.model;

import java.time.LocalDateTime;

public record AdminCommentRecord(
        Long id,
        Long articleId,
        String articleTitle,
        Long parentId,
        String type,
        String contentMarkdown,
        String nickname,
        byte[] emailCiphertext,
        String website,
        String status,
        boolean adminReply,
        String ipSummary,
        LocalDateTime createdAt
) {
}
