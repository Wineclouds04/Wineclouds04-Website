package com.example.blog.operation.dto;

import java.util.List;

public record OperationLogPage(
        List<OperationLogItem> items,
        long total,
        int page,
        int pageSize
) {
}
