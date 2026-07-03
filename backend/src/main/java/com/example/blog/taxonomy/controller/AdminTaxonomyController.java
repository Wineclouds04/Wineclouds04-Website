package com.example.blog.taxonomy.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.taxonomy.dto.TaxonomyItem;
import com.example.blog.taxonomy.dto.TaxonomyRequest;
import com.example.blog.taxonomy.service.TaxonomyService;
import com.example.blog.operation.service.OperationLogService;
import com.example.blog.shared.cache.PublicContentCache;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminTaxonomyController {

    private final TaxonomyService taxonomyService;
    private final OperationLogService operationLogs;
    private final PublicContentCache publicCache;

    public AdminTaxonomyController(
            TaxonomyService taxonomyService,
            OperationLogService operationLogs,
            PublicContentCache publicCache
    ) {
        this.taxonomyService = taxonomyService;
        this.operationLogs = operationLogs;
        this.publicCache = publicCache;
    }

    @GetMapping("/categories")
    List<TaxonomyItem> categories() {
        return taxonomyService.findCategories();
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<TaxonomyItem> createCategory(
            @Valid @RequestBody TaxonomyRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        TaxonomyItem created = taxonomyService.createCategory(request);
        publicCache.invalidateAll();
        operationLogs.record(Long.valueOf(jwt.getSubject()), "TAXONOMY", "CREATE_CATEGORY", created.id(), "{}");
        return ResponseEntity.created(URI.create("/api/v1/admin/categories/" + created.id()))
                .body(created);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    TaxonomyItem updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody TaxonomyRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        TaxonomyItem updated = taxonomyService.updateCategory(id, request);
        publicCache.invalidateAll();
        operationLogs.record(Long.valueOf(jwt.getSubject()), "TAXONOMY", "UPDATE_CATEGORY", id, "{}");
        return updated;
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteCategory(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        taxonomyService.deleteCategory(id);
        publicCache.invalidateAll();
        operationLogs.record(Long.valueOf(jwt.getSubject()), "TAXONOMY", "DELETE_CATEGORY", id, "{}");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tags")
    List<TaxonomyItem> tags() {
        return taxonomyService.findTags();
    }

    @PostMapping("/tags")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<TaxonomyItem> createTag(
            @Valid @RequestBody TaxonomyRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        TaxonomyItem created = taxonomyService.createTag(request);
        publicCache.invalidateAll();
        operationLogs.record(Long.valueOf(jwt.getSubject()), "TAXONOMY", "CREATE_TAG", created.id(), "{}");
        return ResponseEntity.created(URI.create("/api/v1/admin/tags/" + created.id()))
                .body(created);
    }

    @PutMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    TaxonomyItem updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TaxonomyRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        TaxonomyItem updated = taxonomyService.updateTag(id, request);
        publicCache.invalidateAll();
        operationLogs.record(Long.valueOf(jwt.getSubject()), "TAXONOMY", "UPDATE_TAG", id, "{}");
        return updated;
    }

    @DeleteMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteTag(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        taxonomyService.deleteTag(id);
        publicCache.invalidateAll();
        operationLogs.record(Long.valueOf(jwt.getSubject()), "TAXONOMY", "DELETE_TAG", id, "{}");
        return ResponseEntity.noContent().build();
    }
}
