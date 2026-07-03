package com.example.blog.comment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.comment.dto.AdminCommentPage;
import com.example.blog.comment.dto.AdminReplyRequest;
import com.example.blog.comment.dto.PublicCommentResponse;
import com.example.blog.comment.service.CommentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/comments")
public class AdminCommentController {

    private final CommentService service;

    public AdminCommentController(CommentService service) {
        this.service = service;
    }

    @GetMapping
    AdminCommentPage page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return service.adminPage(status, type, keyword, page, pageSize);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.moderate(id, "APPROVED", Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> reject(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.moderate(id, "REJECTED", Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/spam")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> spam(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.moderate(id, "SPAM", Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> hide(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.moderate(id, "HIDDEN", Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    PublicCommentResponse reply(
            @PathVariable Long id,
            @Valid @RequestBody AdminReplyRequest body,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.adminReply(id, body.content(), Long.valueOf(jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
}
