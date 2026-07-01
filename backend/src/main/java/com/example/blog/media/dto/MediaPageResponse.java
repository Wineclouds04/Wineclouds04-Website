package com.example.blog.media.dto;

import java.util.List;

public record MediaPageResponse(
        List<MediaAssetResponse> items,
        long total,
        int page,
        int pageSize
) {
}
