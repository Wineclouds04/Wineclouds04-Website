package com.example.blog.comment.model;

import java.time.LocalDateTime;

public record CommentRecord(
        Long id,
        Long articleId,
        Long rootId,
        Long parentId,
        String type,
        String contentMarkdown,
        String contentHtml,
        String nickname,
        byte[] emailCiphertext,
        String website,
        String anonymousKeyHash,
        String status,
        boolean adminReply,
        boolean notifyOnReply,
        String ipHash,
        String userAgentSummary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
