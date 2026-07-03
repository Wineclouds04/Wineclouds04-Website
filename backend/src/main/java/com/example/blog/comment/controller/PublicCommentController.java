package com.example.blog.comment.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.comment.dto.CommentRequest;
import com.example.blog.comment.dto.CommentSubmitResponse;
import com.example.blog.comment.dto.PublicCommentResponse;
import com.example.blog.comment.service.CommentService;
import com.example.blog.interaction.service.VisitorContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/public")
public class PublicCommentController {

    private final CommentService service;
    private final VisitorContext visitors;

    public PublicCommentController(CommentService service, VisitorContext visitors) {
        this.service = service;
        this.visitors = visitors;
    }

    @GetMapping("/articles/{articleId}/comments")
    List<PublicCommentResponse> articleComments(@PathVariable Long articleId) {
        return service.articleComments(articleId);
    }

    @PostMapping("/articles/{articleId}/comments")
    ResponseEntity<CommentSubmitResponse> submitArticle(
            @PathVariable Long articleId,
            @Valid @RequestBody CommentRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        CommentSubmitResponse result = service.submitArticle(
                articleId,
                body,
                visitors.resolve(request, response)
        );
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/public/comments/" + result.id()))
                .body(result);
    }

    @GetMapping("/messages")
    List<PublicCommentResponse> messages() {
        return service.messages();
    }

    @PostMapping("/messages")
    ResponseEntity<CommentSubmitResponse> submitMessage(
            @Valid @RequestBody CommentRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        CommentSubmitResponse result = service.submitMessage(
                body,
                visitors.resolve(request, response)
        );
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/public/comments/" + result.id()))
                .body(result);
    }
}
