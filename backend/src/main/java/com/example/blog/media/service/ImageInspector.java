package com.example.blog.media.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.example.blog.shared.error.ApiException;

@Component
public class ImageInspector {

    public InspectedImage inspect(
            byte[] content,
            String declaredContentType,
            String originalName,
            int maxWidth,
            int maxHeight
    ) {
        ImageType type = detect(content);
        if (declaredContentType == null
                || !type.contentType().equals(declaredContentType.toLowerCase(Locale.ROOT))) {
            throw invalid("文件 Content-Type 与实际图片格式不一致");
        }
        validateExtension(originalName, type);
        Dimension dimension = type == ImageType.WEBP
                ? webpDimension(content)
                : imageIoDimension(content);
        if (dimension.width() <= 0 || dimension.height() <= 0
                || dimension.width() > maxWidth || dimension.height() > maxHeight) {
            throw invalid("图片尺寸无效或超过限制");
        }
        return new InspectedImage(
                type.extension(),
                type.contentType(),
                dimension.width(),
                dimension.height()
        );
    }

    private ImageType detect(byte[] content) {
        if (content.length >= 12
                && unsigned(content[0]) == 0x89
                && content[1] == 'P' && content[2] == 'N' && content[3] == 'G'
                && unsigned(content[4]) == 0x0D && unsigned(content[5]) == 0x0A
                && unsigned(content[6]) == 0x1A && unsigned(content[7]) == 0x0A) {
            return ImageType.PNG;
        }
        if (content.length >= 3
                && unsigned(content[0]) == 0xFF
                && unsigned(content[1]) == 0xD8
                && unsigned(content[2]) == 0xFF) {
            return ImageType.JPEG;
        }
        if (content.length >= 6) {
            String signature = new String(content, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
            if ("GIF87a".equals(signature) || "GIF89a".equals(signature)) {
                return ImageType.GIF;
            }
        }
        if (content.length >= 30
                && ascii(content, 0, "RIFF")
                && ascii(content, 8, "WEBP")) {
            return ImageType.WEBP;
        }
        throw invalid("仅支持 JPEG、PNG、WebP 和 GIF 图片");
    }

    private Dimension imageIoDimension(byte[] content) {
        try (ImageInputStream stream =
                     ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw invalid("无法读取图片尺寸");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                return new Dimension(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw invalid("图片文件已损坏");
        }
    }

    private Dimension webpDimension(byte[] bytes) {
        String chunk = new String(bytes, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
        if ("VP8X".equals(chunk) && bytes.length >= 30) {
            return new Dimension(
                    1 + littleEndian24(bytes, 24),
                    1 + littleEndian24(bytes, 27)
            );
        }
        if ("VP8 ".equals(chunk) && bytes.length >= 30
                && unsigned(bytes[23]) == 0x9D
                && unsigned(bytes[24]) == 0x01
                && unsigned(bytes[25]) == 0x2A) {
            int width = (unsigned(bytes[26]) | unsigned(bytes[27]) << 8) & 0x3FFF;
            int height = (unsigned(bytes[28]) | unsigned(bytes[29]) << 8) & 0x3FFF;
            return new Dimension(width, height);
        }
        if ("VP8L".equals(chunk) && bytes.length >= 25 && unsigned(bytes[20]) == 0x2F) {
            int bits = unsigned(bytes[21])
                    | unsigned(bytes[22]) << 8
                    | unsigned(bytes[23]) << 16
                    | unsigned(bytes[24]) << 24;
            return new Dimension(1 + (bits & 0x3FFF), 1 + ((bits >> 14) & 0x3FFF));
        }
        throw invalid("无法读取 WebP 图片尺寸");
    }

    private void validateExtension(String originalName, ImageType type) {
        if (originalName == null || !originalName.contains(".")) {
            throw invalid("图片文件缺少扩展名");
        }
        String extension = originalName.substring(originalName.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if (!type.allowedExtensions().contains(extension)) {
            throw invalid("文件扩展名与实际图片格式不一致");
        }
    }

    private boolean ascii(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) return false;
        for (int i = 0; i < value.length(); i++) {
            if (bytes[offset + i] != value.charAt(i)) return false;
        }
        return true;
    }

    private int littleEndian24(byte[] bytes, int offset) {
        return unsigned(bytes[offset])
                | unsigned(bytes[offset + 1]) << 8
                | unsigned(bytes[offset + 2]) << 16;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private enum ImageType {
        JPEG("jpg", "image/jpeg", Set.of("jpg", "jpeg")),
        PNG("png", "image/png", Set.of("png")),
        WEBP("webp", "image/webp", Set.of("webp")),
        GIF("gif", "image/gif", Set.of("gif"));

        private final String extension;
        private final String contentType;
        private final Set<String> allowedExtensions;

        ImageType(String extension, String contentType, Set<String> allowedExtensions) {
            this.extension = extension;
            this.contentType = contentType;
            this.allowedExtensions = allowedExtensions;
        }

        String extension() {
            return extension;
        }

        String contentType() {
            return contentType;
        }

        Set<String> allowedExtensions() {
            return allowedExtensions;
        }
    }

    private record Dimension(int width, int height) {
    }

    public record InspectedImage(
            String extension,
            String contentType,
            int width,
            int height
    ) {
    }
}
