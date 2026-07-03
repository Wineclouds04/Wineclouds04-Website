package com.example.blog.operation.dto;

import java.time.LocalDateTime;

public record OperationLogItem(
        Long id,
        Long operatorId,
        String operatorName,
        String module,
        String action,
        String targetId,
        String result,
        String detailJson,
        String traceId,
        LocalDateTime createdAt
) {
}
