package com.example.blog.taxonomy.dto;

import java.time.LocalDateTime;

public record TaxonomyItem(
        Long id,
        String name,
        String slug,
        String description,
        int sortOrder,
        boolean visible,
        long articleCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
