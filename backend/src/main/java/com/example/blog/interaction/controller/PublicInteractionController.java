package com.example.blog.interaction.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.interaction.dto.InteractionState;
import com.example.blog.interaction.service.InteractionService;
import com.example.blog.interaction.service.VisitorContext;
import com.example.blog.statistics.dto.ViewCountResponse;
import com.example.blog.statistics.service.ViewCounterService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1/public/articles/{articleId}")
public class PublicInteractionController {

    private final InteractionService service;
    private final VisitorContext visitors;
    private final ViewCounterService views;

    public PublicInteractionController(
            InteractionService service,
            VisitorContext visitors,
            ViewCounterService views
    ) {
        this.service = service;
        this.visitors = visitors;
        this.views = views;
    }

    @GetMapping("/interaction")
    InteractionState state(
            @PathVariable Long articleId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var visitor = visitors.resolve(request, response);
        return service.state(articleId, visitor.anonymousKeyHash());
    }

    @PostMapping("/likes")
    InteractionState like(
            @PathVariable Long articleId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var visitor = visitors.resolve(request, response);
        return service.like(articleId, visitor.anonymousKeyHash());
    }

    @DeleteMapping("/likes")
    InteractionState unlike(
            @PathVariable Long articleId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var visitor = visitors.resolve(request, response);
        return service.unlike(articleId, visitor.anonymousKeyHash());
    }

    @PostMapping("/views")
    ViewCountResponse view(
            @PathVariable Long articleId,
            @RequestHeader(value = "Referer", required = false) String referer,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return views.record(articleId, visitors.resolve(request, response), referer);
    }
}
