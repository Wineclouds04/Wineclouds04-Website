package com.example.blog.shared.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.interaction.service.VisitorContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/public/captcha")
public class PublicCaptchaController {

    private final CaptchaService captchas;
    private final VisitorContext visitors;

    public PublicCaptchaController(CaptchaService captchas, VisitorContext visitors) {
        this.captchas = captchas;
        this.visitors = visitors;
    }

    @GetMapping
    CaptchaChallenge challenge(HttpServletRequest request, HttpServletResponse response) {
        var visitor = visitors.resolve(request, response);
        return captchas.issue(visitor.anonymousKeyHash());
    }
}
