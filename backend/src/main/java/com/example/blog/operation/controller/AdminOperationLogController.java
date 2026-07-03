package com.example.blog.operation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.blog.operation.dto.OperationLogPage;
import com.example.blog.operation.service.OperationLogService;

@RestController
@RequestMapping("/api/v1/admin/operation-logs")
public class AdminOperationLogController {

    private final OperationLogService service;

    public AdminOperationLogController(OperationLogService service) {
        this.service = service;
    }

    @GetMapping
    OperationLogPage page(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return service.findPage(module, page, pageSize);
    }
}
