package com.example.blog.shared.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InfrastructureMapper {

    int ping();
}
