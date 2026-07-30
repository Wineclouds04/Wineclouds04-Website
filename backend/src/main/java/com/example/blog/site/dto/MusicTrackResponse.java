package com.example.blog.site.dto;

public record MusicTrackResponse(
        String title,
        String artist,
        String url,
        String coverUrl
) {
}
