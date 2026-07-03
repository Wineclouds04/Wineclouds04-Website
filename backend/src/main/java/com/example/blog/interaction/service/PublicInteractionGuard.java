package com.example.blog.interaction.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.example.blog.shared.security.RateLimitService;

@Service
public class PublicInteractionGuard {

    private final RateLimitService rateLimits;

    public PublicInteractionGuard(RateLimitService rateLimits) {
        this.rateLimits = rateLimits;
    }

    public void comment(String anonymousKeyHash, boolean message) {
        rateLimits.enforce(
                message ? "message" : "comment",
                anonymousKeyHash,
                message ? 2 : 3,
                Duration.ofMinutes(10)
        );
    }

    public void like(String anonymousKeyHash) {
        rateLimits.enforce("like", anonymousKeyHash, 30, Duration.ofHours(1));
    }
}
