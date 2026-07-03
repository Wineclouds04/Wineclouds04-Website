package com.example.blog.article.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import com.example.blog.operation.service.OperationLogService;
import com.example.blog.shared.cache.PublicContentCache;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminArticleController {

    private final ArticleService articleService;
    private final OperationLogService operationLogs;
    private final PublicContentCache publicCache;

    public AdminArticleController(
            ArticleService articleService,
            OperationLogService operationLogs,
            PublicContentCache publicCache
    ) {
        this.articleService = articleService;
        this.operationLogs = operationLogs;
        this.publicCache = publicCache;
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
    ResponseEntity<ArticleDetailResponse> create(
            @Valid @RequestBody ArticleUpsertRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ArticleDetailResponse created = articleService.create(request);
        publicCache.invalidateAll();
        operationLogs.record(Long.valueOf(jwt.getSubject()), "ARTICLE", "CREATE", created.id(), "{}");
        return ResponseEntity.created(URI.create("/api/v1/admin/articles/" + created.id()))
                .body(created);
    }

    @PutMapping("/articles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ArticleDetailResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ArticleUpsertRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ArticleDetailResponse updated = articleService.update(id, request);
        publicCache.invalidateAll();
        operationLogs.record(Long.valueOf(jwt.getSubject()), "ARTICLE", "UPDATE", id, "{}");
        return updated;
    }

    @PostMapping("/articles/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    ArticleDetailResponse publish(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        ArticleDetailResponse published = articleService.publish(id);
        publicCache.invalidateAll();
        operationLogs.record(Long.valueOf(jwt.getSubject()), "ARTICLE", "PUBLISH", id, "{}");
        return published;
    }

    @PostMapping("/articles/{id}/withdraw")
    @PreAuthorize("hasRole('ADMIN')")
    ArticleDetailResponse withdraw(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        ArticleDetailResponse withdrawn = articleService.withdraw(id);
        publicCache.invalidateAll();
        operationLogs.record(Long.valueOf(jwt.getSubject()), "ARTICLE", "WITHDRAW", id, "{}");
        return withdrawn;
    }

    @DeleteMapping("/articles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        articleService.delete(id);
        publicCache.invalidateAll();
        operationLogs.record(Long.valueOf(jwt.getSubject()), "ARTICLE", "DELETE", id, "{}");
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
