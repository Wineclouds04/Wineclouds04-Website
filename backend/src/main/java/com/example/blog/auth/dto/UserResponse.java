package com.example.blog.auth.dto;

import com.example.blog.auth.model.UserAccount;

public record UserResponse(
        String id,
        String username,
        String nickname,
        String email,
        String role
) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.id().toString(),
                user.username(),
                user.nickname(),
                user.email(),
                user.role()
        );
    }
}
