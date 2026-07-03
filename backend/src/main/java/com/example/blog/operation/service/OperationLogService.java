package com.example.blog.operation.service;

import org.springframework.stereotype.Service;
import org.slf4j.MDC;
import com.example.blog.shared.security.RequestTraceFilter;

import com.example.blog.operation.dto.OperationLogPage;
import com.example.blog.operation.mapper.OperationLogMapper;

@Service
public class OperationLogService {

    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void record(Long operatorId, String module, String action, Object targetId, String detailJson) {
        mapper.insert(
                operatorId,
                module,
                action,
                targetId == null ? null : targetId.toString(),
                "SUCCESS",
                detailJson == null ? "{}" : detailJson,
                MDC.get(RequestTraceFilter.TRACE_ID)
        );
    }

    public OperationLogPage findPage(String module, int page, int pageSize) {
        String normalizedModule = module == null || module.isBlank() ? null : module.trim().toUpperCase();
        int safePage = Math.max(1, page);
        int safeSize = Math.clamp(pageSize, 1, 100);
        long offset = (long) (safePage - 1) * safeSize;
        return new OperationLogPage(
                mapper.findPage(normalizedModule, offset, safeSize),
                mapper.count(normalizedModule),
                safePage,
                safeSize
        );
    }
}
