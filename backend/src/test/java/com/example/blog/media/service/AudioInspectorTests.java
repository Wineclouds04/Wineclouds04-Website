package com.example.blog.media.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.example.blog.shared.error.ApiException;

class AudioInspectorTests {

    private final AudioInspector inspector = new AudioInspector();

    @Test
    void acceptsMp3WithId3Header() {
        byte[] content = new byte[] {'I', 'D', '3', 4, 0, 0, 0, 0, 0, 0};

        var result = inspector.inspect(content, "audio/mpeg", "music.mp3");

        assertEquals("mp3", result.extension());
        assertEquals("audio/mpeg", result.contentType());
    }

    @Test
    void acceptsMp3WithMpegFrameHeader() {
        byte[] content = new byte[] {0, 0, (byte) 0xFF, (byte) 0xFB, 0, 0};

        var result = inspector.inspect(content, "audio/mp3", "music.MP3");

        assertEquals("audio/mpeg", result.contentType());
    }

    @Test
    void rejectsMismatchedExtensionOrContent() {
        byte[] content = new byte[] {'I', 'D', '3', 4};

        assertThrows(ApiException.class, () ->
                inspector.inspect(content, "audio/mpeg", "music.wav"));
        assertThrows(ApiException.class, () ->
                inspector.inspect(content, "application/octet-stream", "music.mp3"));
        assertThrows(ApiException.class, () ->
                inspector.inspect(new byte[] {1, 2, 3, 4}, "audio/mpeg", "music.mp3"));
    }
}
