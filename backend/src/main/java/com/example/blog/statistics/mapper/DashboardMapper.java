package com.example.blog.statistics.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.blog.statistics.dto.DashboardPopularArticle;
import com.example.blog.statistics.dto.DashboardTrendPoint;

@Mapper
public interface DashboardMapper {
    long countArticles();
    long countPublishedArticles();
    long countComments();
    long countPendingComments();
    long countMessages();
    long countLikes();
    long sumViews30d();
    long sumVisitors30d();
    List<DashboardTrendPoint> findTrend30d();
    List<DashboardPopularArticle> findPopularArticles(int limit);
}
