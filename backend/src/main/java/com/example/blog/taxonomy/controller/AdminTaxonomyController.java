package com.example.blog.taxonomy.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.taxonomy.dto.TaxonomyItem;
import com.example.blog.taxonomy.dto.TaxonomyRequest;
import com.example.blog.taxonomy.service.TaxonomyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminTaxonomyController {

    private final TaxonomyService taxonomyService;

    public AdminTaxonomyController(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    @GetMapping("/categories")
    List<TaxonomyItem> categories() {
        return taxonomyService.findCategories();
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<TaxonomyItem> createCategory(@Valid @RequestBody TaxonomyRequest request) {
        TaxonomyItem created = taxonomyService.createCategory(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/categories/" + created.id()))
                .body(created);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    TaxonomyItem updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody TaxonomyRequest request
    ) {
        return taxonomyService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        taxonomyService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tags")
    List<TaxonomyItem> tags() {
        return taxonomyService.findTags();
    }

    @PostMapping("/tags")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<TaxonomyItem> createTag(@Valid @RequestBody TaxonomyRequest request) {
        TaxonomyItem created = taxonomyService.createTag(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/tags/" + created.id()))
                .body(created);
    }

    @PutMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    TaxonomyItem updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TaxonomyRequest request
    ) {
        return taxonomyService.updateTag(id, request);
    }

    @DeleteMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        taxonomyService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
