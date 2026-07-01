package com.example.blog.media.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.blog.media.config.OssProperties;
import com.example.blog.media.dto.MediaAssetResponse;
import com.example.blog.media.dto.MediaConfigResponse;
import com.example.blog.media.dto.MediaPageResponse;
import com.example.blog.media.mapper.MediaMapper;
import com.example.blog.media.model.MediaAssetRecord;
import com.example.blog.media.service.ImageInspector.InspectedImage;
import com.example.blog.media.storage.ObjectStorage;
import com.example.blog.shared.error.ApiException;

@Service
public class MediaService {

    private final MediaMapper mediaMapper;
    private final ObjectStorage objectStorage;
    private final ImageInspector imageInspector;
    private final OssProperties properties;

    public MediaService(
            MediaMapper mediaMapper,
            ObjectStorage objectStorage,
            ImageInspector imageInspector,
            OssProperties properties
    ) {
        this.mediaMapper = mediaMapper;
        this.objectStorage = objectStorage;
        this.imageInspector = imageInspector;
        this.properties = properties;
    }

    public MediaConfigResponse config() {
        return new MediaConfigResponse(objectStorage.configured(), properties.maxImageSize());
    }

    public MediaPageResponse findPage(int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.clamp(pageSize, 1, 100);
        long offset = (long) (safePage - 1) * safePageSize;
        return new MediaPageResponse(
                mediaMapper.findPage(offset, safePageSize).stream().map(this::toResponse).toList(),
                mediaMapper.count(),
                safePage,
                safePageSize
        );
    }

    @Transactional
    public MediaAssetResponse upload(MultipartFile file, String altText, Long userId) {
        if (!objectStorage.configured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OSS 尚未配置");
        }
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择要上传的图片");
        }
        if (file.getSize() > properties.maxImageSize()) {
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE, "图片超过上传大小限制");
        }
        String originalName = safeOriginalName(file.getOriginalFilename());
        String normalizedAlt = blankToNull(altText);
        if (normalizedAlt != null && normalizedAlt.length() > 300) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "图片替代文本不能超过 300 个字符");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (java.io.IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "无法读取上传文件");
        }
        InspectedImage image = imageInspector.inspect(
                content,
                file.getContentType(),
                originalName,
                properties.maxImageWidth(),
                properties.maxImageHeight()
        );
        String objectKey = objectKey(image.extension());
        objectStorage.put(objectKey, content, image.contentType());
        try {
            mediaMapper.insert(
                    objectKey,
                    originalName,
                    image.contentType(),
                    image.extension(),
                    content.length,
                    image.width(),
                    image.height(),
                    sha256(content),
                    normalizedAlt,
                    userId
            );
            return findById(mediaMapper.lastInsertId());
        } catch (RuntimeException exception) {
            try {
                objectStorage.delete(objectKey);
            } catch (RuntimeException ignored) {
                // The orphan can be reconciled from OSS inventory if compensation also fails.
            }
            throw exception;
        }
    }

    @Transactional
    public MediaAssetResponse updateAltText(Long id, String altText) {
        require(id);
        if (mediaMapper.updateAltText(id, blankToNull(altText)) == 0) {
            throw notFound();
        }
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        MediaAssetRecord asset = require(id);
        if (asset.referenceCount() > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "媒体仍被文章引用，不能删除");
        }
        if (mediaMapper.softDelete(id) == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "媒体仍被引用或已经删除");
        }
        objectStorage.delete(asset.objectKey());
    }

    private MediaAssetResponse findById(Long id) {
        return toResponse(require(id));
    }

    private MediaAssetRecord require(Long id) {
        return mediaMapper.findById(id).orElseThrow(this::notFound);
    }

    private MediaAssetResponse toResponse(MediaAssetRecord asset) {
        return new MediaAssetResponse(
                asset.id(),
                asset.originalName(),
                asset.mediaType(),
                asset.extension(),
                asset.sizeBytes(),
                asset.width(),
                asset.height(),
                asset.altText(),
                asset.status(),
                asset.referenceCount(),
                objectStorage.publicUrl(asset.objectKey()),
                asset.createdAt()
        );
    }

    private String objectKey(String extension) {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        String prefix = properties.objectPrefix() == null || properties.objectPrefix().isBlank()
                ? "blog/dev"
                : properties.objectPrefix().replaceAll("^/+|/+$", "");
        return "%s/%04d/%02d/%s.%s".formatted(
                prefix,
                now.getYear(),
                now.getMonthValue(),
                UUID.randomUUID(),
                extension
        );
    }

    private String safeOriginalName(String name) {
        if (name == null || name.isBlank()) return "image";
        String safe = name.replace('\\', '/');
        safe = safe.substring(safe.lastIndexOf('/') + 1).trim();
        if (safe.length() > 255) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "原始文件名过长");
        }
        return safe;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "媒体不存在");
    }
}
