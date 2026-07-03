package com.example.blog.comment.dto;

import java.util.List;

public record AdminCommentPage(
        List<AdminCommentItem> items,
        long total,
        int page,
        int pageSize
) {
}
