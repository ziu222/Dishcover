package com.dishcover.image.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageResizerTest {

    private final ImageResizer resizer = new ImageResizer();

    private byte[] pngOf(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void downscalesLargeImageAndKeepsAspectRatio() throws IOException {
        byte[] big = pngOf(2000, 1000);

        ImageResizer.ResizedImage result = resizer.resize(big, "image/png");

        assertEquals("image/jpeg", result.mimeType());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertNotNull(decoded);
        assertEquals(1024, Math.max(decoded.getWidth(), decoded.getHeight()));
        assertEquals(512, Math.min(decoded.getWidth(), decoded.getHeight())); // giữ tỉ lệ 2:1
    }

    @Test
    void passesThroughImageAlreadySmallEnough() throws IOException {
        byte[] small = pngOf(800, 600);

        ImageResizer.ResizedImage result = resizer.resize(small, "image/png");

        assertSame(small, result.bytes()); // không re-encode khi đã đủ nhỏ
        assertEquals("image/png", result.mimeType());
    }

    @Test
    void passesThroughUndecodableBytesKeepingOriginalMime() {
        byte[] notAnImage = "RIFF????WEBPfake".getBytes();

        ImageResizer.ResizedImage result = resizer.resize(notAnImage, "image/webp");

        assertSame(notAnImage, result.bytes());
        assertEquals("image/webp", result.mimeType());
    }
}
