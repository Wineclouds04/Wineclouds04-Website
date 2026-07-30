package com.example.blog.site.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.shared.error.ApiException;
import com.example.blog.site.dto.MusicTrackRequest;
import com.example.blog.site.dto.MusicTrackResponse;
import com.example.blog.site.dto.SiteProfileResponse;
import com.example.blog.site.dto.SiteProfileUpdateRequest;
import com.example.blog.site.mapper.SiteProfileMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class SiteProfileService {

    private static final String DEFAULT_AVATAR_URL = "/images/wineclouds-avatar.webp";
    private static final String DEFAULT_SIGNATURE = "本质哈基米";
    private static final String DEFAULT_MUSIC_TITLE = "未命名曲目";
    private static final String DEFAULT_MUSIC_ARTIST = "Wineclouds";
    private static final int DEFAULT_MUSIC_VOLUME = 100;

    private final SiteProfileMapper mapper;
    private final ObjectMapper objectMapper;

    public SiteProfileService(SiteProfileMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public SiteProfileResponse publicProfile() {
        return mapper.findProfileJson()
                .flatMap(this::readProfile)
                .orElseGet(this::defaults);
    }

    @Transactional
    public SiteProfileResponse update(SiteProfileUpdateRequest request) {
        String avatarUrl = normalizeAvatarUrl(request.avatarUrl());
        String signature = normalizeSignature(request.signature());
        boolean musicEnabled = request.musicEnabled();
        int musicVolume = normalizeMusicVolume(request.musicVolume());
        List<MusicTrackResponse> musicPlaylist = musicEnabled
                ? normalizeMusicPlaylist(request.musicPlaylist(), request, avatarUrl)
                : List.of();
        MusicTrackResponse primaryTrack = musicPlaylist.isEmpty()
                ? new MusicTrackResponse("", "", "", "")
                : musicPlaylist.getFirst();
        SiteProfileResponse profile = new SiteProfileResponse(
                avatarUrl,
                signature,
                musicEnabled && !musicPlaylist.isEmpty(),
                primaryTrack.title(),
                primaryTrack.artist(),
                primaryTrack.url(),
                primaryTrack.coverUrl(),
                musicPlaylist,
                musicVolume
        );
        mapper.upsertProfileJson(writeProfile(profile));
        return profile;
    }

    private Optional<SiteProfileResponse> readProfile(String profileJson) {
        try {
            JsonNode node = objectMapper.readTree(profileJson);
            if (!node.isObject()) return Optional.empty();
            String avatarUrl = normalizeStoredAvatarUrl(node.path("avatarUrl").asText(null));
            String signature = normalizeStoredSignature(node.path("signature").asText(null));
            boolean musicEnabled = node.path("musicEnabled").asBoolean(false);
            String musicTitle = musicEnabled
                    ? normalizeStoredMusicText(node.path("musicTitle").asText(null), DEFAULT_MUSIC_TITLE)
                    : "";
            String musicArtist = musicEnabled
                    ? normalizeStoredMusicText(node.path("musicArtist").asText(null), DEFAULT_MUSIC_ARTIST)
                    : "";
            String musicUrl = musicEnabled ? normalizeStoredMusicUrl(node.path("musicUrl").asText(null)) : "";
            String musicCoverUrl = musicEnabled
                    ? normalizeStoredMusicCoverUrl(node.path("musicCoverUrl").asText(null), avatarUrl)
                    : "";
            List<MusicTrackResponse> musicPlaylist = musicEnabled
                    ? normalizeStoredMusicPlaylist(node.path("musicPlaylist"), musicTitle, musicArtist, musicUrl, musicCoverUrl, avatarUrl)
                    : List.of();
            int musicVolume = normalizeStoredMusicVolume(node.path("musicVolume"));
            MusicTrackResponse primaryTrack = musicPlaylist.isEmpty()
                    ? new MusicTrackResponse("", "", "", "")
                    : musicPlaylist.getFirst();
            boolean playableMusic = musicEnabled && !musicPlaylist.isEmpty();
            return Optional.of(new SiteProfileResponse(
                    avatarUrl,
                    signature,
                    playableMusic,
                    playableMusic ? primaryTrack.title() : "",
                    playableMusic ? primaryTrack.artist() : "",
                    playableMusic ? primaryTrack.url() : "",
                    playableMusic ? primaryTrack.coverUrl() : "",
                    playableMusic ? musicPlaylist : List.of(),
                    musicVolume
            ));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private String writeProfile(SiteProfileResponse profile) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("avatarUrl", profile.avatarUrl());
        node.put("signature", profile.signature());
        node.put("musicEnabled", profile.musicEnabled());
        node.put("musicTitle", profile.musicTitle());
        node.put("musicArtist", profile.musicArtist());
        node.put("musicUrl", profile.musicUrl());
        node.put("musicCoverUrl", profile.musicCoverUrl());
        node.put("musicVolume", profile.musicVolume());
        node.set("musicPlaylist", objectMapper.valueToTree(profile.musicPlaylist()));
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化个人资料", exception);
        }
    }

    private String normalizeAvatarUrl(String value) {
        String avatarUrl = value == null ? "" : value.trim();
        if (avatarUrl.isEmpty()) return DEFAULT_AVATAR_URL;
        if (avatarUrl.startsWith("/") && !avatarUrl.startsWith("//")) return avatarUrl;
        try {
            URI uri = URI.create(avatarUrl);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) return avatarUrl;
        } catch (IllegalArgumentException ignored) {
            // Return the same public validation error for malformed and unsafe values.
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "头像地址只能使用站内路径或 HTTPS 链接");
    }

    private String normalizeSignature(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "签名不能为空");
        }
        String signature = value.trim();
        if (signature.length() > 160) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "签名不能超过 160 个字符");
        }
        return signature;
    }

    private String normalizeMusicText(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) return fallback;
        if (text.length() > 120) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "音乐资料不能超过 120 个字符");
        }
        return text;
    }

    private String normalizeMusicUrl(String value) {
        String url = value == null ? "" : value.trim();
        if (url.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "启用播放器时必须填写音乐地址");
        }
        return normalizePublicUrl(url, "音乐地址只能使用站内路径或 HTTPS 链接");
    }

    private String normalizeMusicCoverUrl(String value, String fallback) {
        String url = value == null ? "" : value.trim();
        if (url.isEmpty()) return fallback;
        return normalizePublicUrl(url, "音乐封面只能使用站内路径或 HTTPS 链接");
    }

    private List<MusicTrackResponse> normalizeMusicPlaylist(
            List<MusicTrackRequest> playlist,
            SiteProfileUpdateRequest request,
            String avatarUrl
    ) {
        List<MusicTrackResponse> tracks = new ArrayList<>();
        if (playlist != null) {
            for (MusicTrackRequest track : playlist) {
                if (track == null) continue;
                if (isBlank(track.title()) && isBlank(track.artist()) && isBlank(track.url()) && isBlank(track.coverUrl())) {
                    continue;
                }
                tracks.add(normalizeMusicTrack(track.title(), track.artist(), track.url(), track.coverUrl(), avatarUrl));
            }
        }
        if (tracks.isEmpty()) {
            tracks.add(normalizeMusicTrack(request.musicTitle(), request.musicArtist(), request.musicUrl(), request.musicCoverUrl(), avatarUrl));
        }
        return List.copyOf(tracks);
    }

    private MusicTrackResponse normalizeMusicTrack(String title, String artist, String url, String coverUrl, String avatarUrl) {
        return new MusicTrackResponse(
                normalizeMusicText(title, DEFAULT_MUSIC_TITLE),
                normalizeMusicText(artist, DEFAULT_MUSIC_ARTIST),
                normalizeMusicUrl(url),
                normalizeMusicCoverUrl(coverUrl, avatarUrl)
        );
    }

    private int normalizeMusicVolume(Integer value) {
        if (value == null) return DEFAULT_MUSIC_VOLUME;
        return Math.max(0, Math.min(100, value));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeStoredAvatarUrl(String value) {
        try {
            return normalizeAvatarUrl(value);
        } catch (ApiException exception) {
            return DEFAULT_AVATAR_URL;
        }
    }

    private String normalizeStoredSignature(String value) {
        try {
            return normalizeSignature(value);
        } catch (ApiException exception) {
            return DEFAULT_SIGNATURE;
        }
    }

    private String normalizeStoredMusicText(String value, String fallback) {
        try {
            return normalizeMusicText(value, fallback);
        } catch (ApiException exception) {
            return fallback;
        }
    }

    private String normalizeStoredMusicUrl(String value) {
        try {
            return normalizeMusicUrl(value);
        } catch (ApiException exception) {
            return "";
        }
    }

    private String normalizeStoredMusicCoverUrl(String value, String fallback) {
        try {
            return normalizeMusicCoverUrl(value, fallback);
        } catch (ApiException exception) {
            return fallback;
        }
    }

    private List<MusicTrackResponse> normalizeStoredMusicPlaylist(
            JsonNode playlistNode,
            String fallbackTitle,
            String fallbackArtist,
            String fallbackUrl,
            String fallbackCoverUrl,
            String avatarUrl
    ) {
        List<MusicTrackResponse> tracks = new ArrayList<>();
        if (playlistNode.isArray()) {
            for (JsonNode trackNode : playlistNode) {
                if (!trackNode.isObject()) continue;
                String url = normalizeStoredMusicUrl(trackNode.path("url").asText(null));
                if (url.isEmpty()) continue;
                String title = normalizeStoredMusicText(trackNode.path("title").asText(null), DEFAULT_MUSIC_TITLE);
                String artist = normalizeStoredMusicText(trackNode.path("artist").asText(null), DEFAULT_MUSIC_ARTIST);
                String coverUrl = normalizeStoredMusicCoverUrl(trackNode.path("coverUrl").asText(null), avatarUrl);
                tracks.add(new MusicTrackResponse(title, artist, url, coverUrl));
            }
        }
        if (tracks.isEmpty() && !fallbackUrl.isEmpty()) {
            tracks.add(new MusicTrackResponse(fallbackTitle, fallbackArtist, fallbackUrl, fallbackCoverUrl));
        }
        return List.copyOf(tracks);
    }

    private int normalizeStoredMusicVolume(JsonNode node) {
        if (!node.isInt()) return DEFAULT_MUSIC_VOLUME;
        return normalizeMusicVolume(node.asInt());
    }

    private String normalizePublicUrl(String value, String errorMessage) {
        if (value.startsWith("/") && !value.startsWith("//")) return value;
        try {
            URI uri = URI.create(value);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) return value;
        } catch (IllegalArgumentException ignored) {
            // Return the same public validation error for malformed and unsafe values.
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, errorMessage);
    }

    private SiteProfileResponse defaults() {
        return new SiteProfileResponse(
                DEFAULT_AVATAR_URL,
                DEFAULT_SIGNATURE,
                false,
                "",
                "",
                "",
                "",
                List.of(),
                DEFAULT_MUSIC_VOLUME
        );
    }
}
