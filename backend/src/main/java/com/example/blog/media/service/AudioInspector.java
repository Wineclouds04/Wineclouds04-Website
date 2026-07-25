package com.example.blog.media.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.example.blog.shared.error.ApiException;

@Component
public class AudioInspector {

    private static final Set<String> MP3_CONTENT_TYPES = Set.of("audio/mpeg", "audio/mp3");
    private static final int FRAME_SCAN_LIMIT = 64 * 1024;

    public InspectedAudio inspect(byte[] content, String declaredContentType, String originalName) {
        validateExtension(originalName);
        String contentType = declaredContentType == null
                ? ""
                : declaredContentType.toLowerCase(Locale.ROOT);
        if (!MP3_CONTENT_TYPES.contains(contentType)) {
            throw invalid("文件 Content-Type 必须为 audio/mpeg");
        }
        if (!containsMp3Header(content)) {
            throw invalid("文件内容不是有效的 MP3 音频");
        }
        return new InspectedAudio("mp3", "audio/mpeg");
    }

    private void validateExtension(String originalName) {
        if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
            throw invalid("音乐文件必须使用 .mp3 扩展名");
        }
    }

    private boolean containsMp3Header(byte[] content) {
        if (content.length < 4) return false;
        if (content[0] == 'I' && content[1] == 'D' && content[2] == '3') return true;

        int limit = Math.min(content.length - 1, FRAME_SCAN_LIMIT);
        for (int index = 0; index < limit; index++) {
            int first = content[index] & 0xFF;
            int second = content[index + 1] & 0xFF;
            if (first == 0xFF && (second & 0xE0) == 0xE0 && (second & 0x06) != 0) {
                return true;
            }
        }
        return false;
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public record InspectedAudio(String extension, String contentType) {
    }
}
