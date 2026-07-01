package com.example.blog.article.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.article.dto.ArticleDetailResponse;
import com.example.blog.article.dto.ArticlePageResponse;
import com.example.blog.article.dto.ArticleUpsertRequest;
import com.example.blog.article.dto.TaxonomyOption;
import com.example.blog.article.service.ArticleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminArticleController {

    private final ArticleService articleService;

    public AdminArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/articles")
    ArticlePageResponse articles(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return articleService.findPage(status, keyword, page, pageSize);
    }

    @GetMapping("/articles/{id}")
    ArticleDetailResponse article(@PathVariable Long id) {
        return articleService.findById(id);
    }

    @PostMapping("/articles")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ArticleDetailResponse> create(@Valid @RequestBody ArticleUpsertRequest request) {
        ArticleDetailResponse created = articleService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/articles/" + created.id()))
                .body(created);
    }

    @PutMapping("/articles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ArticleDetailResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ArticleUpsertRequest request
    ) {
        return articleService.update(id, request);
    }

    @PostMapping("/articles/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    ArticleDetailResponse publish(@PathVariable Long id) {
        return articleService.publish(id);
    }

    @PostMapping("/articles/{id}/withdraw")
    @PreAuthorize("hasRole('ADMIN')")
    ArticleDetailResponse withdraw(@PathVariable Long id) {
        return articleService.withdraw(id);
    }

    @DeleteMapping("/articles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories/options")
    List<TaxonomyOption> categories() {
        return articleService.findCategories();
    }

    @GetMapping("/tags/options")
    List<TaxonomyOption> tags() {
        return articleService.findTags();
    }
}
