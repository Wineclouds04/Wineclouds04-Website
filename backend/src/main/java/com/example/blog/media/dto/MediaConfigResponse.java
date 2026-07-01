package com.example.blog.media.dto;

public record MediaConfigResponse(
        boolean configured,
        long maxImageSize
) {
}
