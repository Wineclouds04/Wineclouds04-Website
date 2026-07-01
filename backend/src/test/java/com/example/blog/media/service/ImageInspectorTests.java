package com.example.blog.media.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.example.blog.shared.error.ApiException;

class ImageInspectorTests {

    private final ImageInspector inspector = new ImageInspector();

    @Test
    void readsPngMetadataWithoutTrustingTheFilename() throws Exception {
        BufferedImage image = new BufferedImage(32, 18, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        var result = inspector.inspect(
                output.toByteArray(),
                "image/png",
                "cover.png",
                100,
                100
        );

        assertEquals("png", result.extension());
        assertEquals(32, result.width());
        assertEquals(18, result.height());
    }

    @Test
    void rejectsAContentTypeThatDoesNotMatchTheMagicBytes() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        assertThrows(
                ApiException.class,
                () -> inspector.inspect(
                        output.toByteArray(),
                        "image/jpeg",
                        "fake.jpg",
                        100,
                        100
                )
        );
    }

    @Test
    void readsWebpExtendedHeaderDimensions() {
        byte[] webp = new byte[30];
        writeAscii(webp, 0, "RIFF");
        writeAscii(webp, 8, "WEBP");
        writeAscii(webp, 12, "VP8X");
        writeLittleEndian24(webp, 24, 639);
        writeLittleEndian24(webp, 27, 359);

        var result = inspector.inspect(webp, "image/webp", "hero.webp", 1000, 1000);

        assertEquals(640, result.width());
        assertEquals(360, result.height());
    }

    private void writeAscii(byte[] target, int offset, String value) {
        for (int index = 0; index < value.length(); index++) {
            target[offset + index] = (byte) value.charAt(index);
        }
    }

    private void writeLittleEndian24(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >> 8);
        target[offset + 2] = (byte) (value >> 16);
    }
}
