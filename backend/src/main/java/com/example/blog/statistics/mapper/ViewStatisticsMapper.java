package com.example.blog.statistics.mapper;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ViewStatisticsMapper {

    boolean articleIsPublic(Long articleId);

    int addArticleViews(
            @Param("articleId") Long articleId,
            @Param("statDate") LocalDate statDate,
            @Param("viewDelta") long viewDelta,
            @Param("uniqueAbsolute") long uniqueAbsolute
    );

    int addArticleTotal(@Param("articleId") Long articleId, @Param("delta") long delta);

    int addSiteViews(
            @Param("statDate") LocalDate statDate,
            @Param("viewDelta") long viewDelta,
            @Param("uniqueAbsolute") long uniqueAbsolute,
            @Param("directDelta") long directDelta,
            @Param("searchDelta") long searchDelta,
            @Param("referralDelta") long referralDelta
    );
}
