package com.example.blog.statistics.service;

import org.springframework.stereotype.Service;

import com.example.blog.notification.mapper.NotificationOutboxMapper;
import com.example.blog.notification.service.MailOutboxProcessor;
import com.example.blog.operation.mapper.OperationLogMapper;
import com.example.blog.statistics.dto.DashboardResponse;
import com.example.blog.statistics.mapper.DashboardMapper;
import com.example.blog.shared.cache.CacheMetrics;

@Service
public class DashboardService {

    private final DashboardMapper mapper;
    private final OperationLogMapper operationLogs;
    private final NotificationOutboxMapper outbox;
    private final MailOutboxProcessor mail;
    private final CacheMetrics cacheMetrics;

    public DashboardService(
            DashboardMapper mapper,
            OperationLogMapper operationLogs,
            NotificationOutboxMapper outbox,
            MailOutboxProcessor mail,
            CacheMetrics cacheMetrics
    ) {
        this.mapper = mapper;
        this.operationLogs = operationLogs;
        this.outbox = outbox;
        this.mail = mail;
        this.cacheMetrics = cacheMetrics;
    }

    public DashboardResponse dashboard() {
        var cache = cacheMetrics.snapshot();
        return new DashboardResponse(
                mapper.countArticles(),
                mapper.countPublishedArticles(),
                mapper.countComments(),
                mapper.countPendingComments(),
                mapper.countMessages(),
                mapper.countLikes(),
                mapper.sumViews30d(),
                mapper.sumVisitors30d(),
                mapper.findTrend30d(),
                mapper.findPopularArticles(5),
                operationLogs.findRecent(8),
                outbox.countPending(),
                mail.configured(),
                "UP",
                cache.hits(),
                cache.misses(),
                cache.errors(),
                cache.hitRate()
        );
    }
}
