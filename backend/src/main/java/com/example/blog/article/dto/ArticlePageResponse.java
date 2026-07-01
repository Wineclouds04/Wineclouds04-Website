package com.example.blog.article.dto;

import java.util.List;

public record ArticlePageResponse(
        List<ArticleListItem> items,
        long total,
        int page,
        int pageSize
) {
}
