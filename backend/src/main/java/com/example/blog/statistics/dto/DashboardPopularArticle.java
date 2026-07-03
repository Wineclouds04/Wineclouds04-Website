package com.example.blog.statistics.dto;

public record DashboardPopularArticle(
        Long id,
        String title,
        String slug,
        long views,
        long likes,
        long comments
) {
}
