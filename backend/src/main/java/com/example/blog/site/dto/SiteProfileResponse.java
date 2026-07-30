package com.example.blog.site.dto;

import java.util.List;

public record SiteProfileResponse(
        String avatarUrl,
        String signature,
        boolean musicEnabled,
        String musicTitle,
        String musicArtist,
        String musicUrl,
        String musicCoverUrl,
        List<MusicTrackResponse> musicPlaylist,
        int musicVolume
) {
}
