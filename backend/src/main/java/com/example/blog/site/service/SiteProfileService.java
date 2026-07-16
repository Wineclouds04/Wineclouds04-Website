package com.example.blog.site.service;

import java.net.URI;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.shared.error.ApiException;
import com.example.blog.site.dto.SiteProfileResponse;
import com.example.blog.site.dto.SiteProfileUpdateRequest;
import com.example.blog.site.mapper.SiteProfileMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class SiteProfileService {

    private static final String DEFAULT_AVATAR_URL = "/images/wineclouds-avatar.png";
    private static final String DEFAULT_SIGNATURE = "本质哈基米";

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
        SiteProfileResponse profile = new SiteProfileResponse(avatarUrl, signature);
        mapper.upsertProfileJson(writeProfile(profile));
        return profile;
    }

    private Optional<SiteProfileResponse> readProfile(String profileJson) {
        try {
            JsonNode node = objectMapper.readTree(profileJson);
            if (!node.isObject()) return Optional.empty();
            String avatarUrl = normalizeStoredAvatarUrl(node.path("avatarUrl").asText(null));
            String signature = normalizeStoredSignature(node.path("signature").asText(null));
            return Optional.of(new SiteProfileResponse(avatarUrl, signature));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private String writeProfile(SiteProfileResponse profile) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("avatarUrl", profile.avatarUrl());
        node.put("signature", profile.signature());
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

    private SiteProfileResponse defaults() {
        return new SiteProfileResponse(DEFAULT_AVATAR_URL, DEFAULT_SIGNATURE);
    }
}
