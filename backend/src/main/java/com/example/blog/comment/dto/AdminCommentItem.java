package com.example.blog.comment.dto;

import java.time.LocalDateTime;

public record AdminCommentItem(
        Long id,
        Long articleId,
        String articleTitle,
        Long parentId,
        String type,
        String contentMarkdown,
        String nickname,
        String emailMasked,
        String website,
        String status,
        boolean adminReply,
        String ipSummary,
        LocalDateTime createdAt
) {
}
