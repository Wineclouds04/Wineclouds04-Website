package com.example.blog.statistics.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SiteStatisticsOverrideMapper {

    Optional<String> findOverrideJson();

    int upsertOverrideJson(@Param("overrideJson") String overrideJson);
}
