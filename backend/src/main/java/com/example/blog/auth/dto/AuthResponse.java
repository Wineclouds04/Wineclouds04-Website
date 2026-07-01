package com.example.blog.auth.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        Instant expiresAt,
        UserResponse user
) {
}
