package com.example.blog.site.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SiteProfileMapper {

    Optional<String> findProfileJson();

    int upsertProfileJson(@Param("profileJson") String profileJson);
}
