package com.example.blog.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicCommentResponse(
        Long id,
        Long parentId,
        String nickname,
        String contentHtml,
        boolean adminReply,
        LocalDateTime createdAt,
        List<PublicCommentResponse> replies
) {
}
