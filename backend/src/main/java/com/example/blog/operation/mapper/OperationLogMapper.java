package com.example.blog.operation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.blog.operation.dto.OperationLogItem;

@Mapper
public interface OperationLogMapper {

    int insert(
            @Param("operatorId") Long operatorId,
            @Param("module") String module,
            @Param("action") String action,
            @Param("targetId") String targetId,
            @Param("result") String result,
            @Param("detailJson") String detailJson,
            @Param("traceId") String traceId
    );

    List<OperationLogItem> findPage(
            @Param("module") String module,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    long count(@Param("module") String module);

    List<OperationLogItem> findRecent(int limit);
}
