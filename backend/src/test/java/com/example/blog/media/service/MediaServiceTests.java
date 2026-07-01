package com.example.blog.media.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.example.blog.media.config.OssProperties;
import com.example.blog.media.mapper.MediaMapper;
import com.example.blog.media.model.MediaAssetRecord;
import com.example.blog.media.storage.ObjectStorage;

@ExtendWith(MockitoExtension.class)
class MediaServiceTests {

    @Mock
    private MediaMapper mediaMapper;

    @Mock
    private ObjectStorage objectStorage;

    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        var properties = new OssProperties(
                "cn-hangzhou",
                "https://oss-cn-hangzhou.aliyuncs.com",
                "bucket",
                "https://cdn.example.com",
                "blog/test",
                10 * 1024 * 1024,
                12000,
                12000
        );
        mediaService = new MediaService(
                mediaMapper,
                objectStorage,
                new ImageInspector(),
                properties
        );
    }

    @Test
    void validatesUploadsAndPersistsMetadata() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(24, 12, BufferedImage.TYPE_INT_RGB), "png", output);
        byte[] bytes = output.toByteArray();
        var file = new MockMultipartFile("file", "cover.png", "image/png", bytes);
        var record = new MediaAssetRecord(
                9L,
                "blog/test/2026/07/image.png",
                "cover.png",
                "image/png",
                "png",
                bytes.length,
                24,
                12,
                "0".repeat(64),
                "Cover",
                "PENDING",
                0,
                1L,
                LocalDateTime.of(2026, 7, 1, 8, 0)
        );
        when(objectStorage.configured()).thenReturn(true);
        when(mediaMapper.lastInsertId()).thenReturn(9L);
        when(mediaMapper.findById(9L)).thenReturn(Optional.of(record));
        when(objectStorage.publicUrl(record.objectKey()))
                .thenReturn("https://cdn.example.com/" + record.objectKey());

        var uploaded = mediaService.upload(file, "Cover", 1L);

        assertEquals(24, uploaded.width());
        assertEquals("https://cdn.example.com/" + record.objectKey(), uploaded.url());
        verify(objectStorage).put(anyString(), eq(bytes), eq("image/png"));
        verify(mediaMapper).insert(
                anyString(),
                eq("cover.png"),
                eq("image/png"),
                eq("png"),
                eq((long) bytes.length),
                eq(24),
                eq(12),
                anyString(),
                eq("Cover"),
                eq(1L)
        );
    }
}
