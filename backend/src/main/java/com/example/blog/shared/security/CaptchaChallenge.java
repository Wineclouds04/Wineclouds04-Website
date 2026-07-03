package com.example.blog.shared.security;

import java.time.Instant;

public record CaptchaChallenge(String id, String question, Instant expiresAt) {
}
