package com.example.blog.article.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.article.dto.MarkdownPreviewRequest;
import com.example.blog.article.dto.MarkdownPreviewResponse;
import com.example.blog.article.service.MarkdownService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/markdown")
public class AdminMarkdownController {

    private final MarkdownService markdownService;

    public AdminMarkdownController(MarkdownService markdownService) {
        this.markdownService = markdownService;
    }

    @PostMapping("/preview")
    MarkdownPreviewResponse preview(@Valid @RequestBody MarkdownPreviewRequest request) {
        var rendered = markdownService.render(request.markdown());
        return new MarkdownPreviewResponse(
                rendered.html(),
                rendered.plain(),
                rendered.wordCount(),
                rendered.readingMinutes()
        );
    }
}
