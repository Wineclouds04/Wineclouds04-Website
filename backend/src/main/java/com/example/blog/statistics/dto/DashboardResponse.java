package com.example.blog.statistics.dto;

import java.util.List;

import com.example.blog.operation.dto.OperationLogItem;

public record DashboardResponse(
        long articles,
        long publishedArticles,
        long comments,
        long pendingComments,
        long messages,
        long likes,
        long views30d,
        long visitors30d,
        List<DashboardTrendPoint> trend,
        List<DashboardPopularArticle> popularArticles,
        List<OperationLogItem> recentOperations,
        long pendingNotifications,
        boolean mailConfigured,
        String serviceStatus,
        long cacheHits,
        long cacheMisses,
        long cacheErrors,
        double cacheHitRate
) {
}
