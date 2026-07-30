package com.example.blog.site.dto;

import jakarta.validation.constraints.Size;

public record MusicTrackRequest(
        @Size(max = 120) String title,
        @Size(max = 120) String artist,
        @Size(max = 1000) String url,
        @Size(max = 1000) String coverUrl
) {
}
