package com.dishcover.image.service;

import com.dishcover.image.exception.InvalidImageException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageValidatorTest {

    private final ImageValidator validator = new ImageValidator();

    @Test
    void acceptsValidPng() {
        assertDoesNotThrow(() -> validator.validate(new byte[]{1, 2, 3}, "image/png"));
    }

    @Test
    void acceptsContentTypeWithDifferentCase() {
        assertDoesNotThrow(() -> validator.validate(new byte[]{1}, "IMAGE/JPEG"));
    }

    @Test
    void rejectsEmptyBytes() {
        assertThrows(InvalidImageException.class,
                () -> validator.validate(new byte[0], "image/png"));
    }

    @Test
    void rejectsUnsupportedFormat() {
        assertThrows(InvalidImageException.class,
                () -> validator.validate(new byte[]{1}, "image/gif"));
    }

    @Test
    void rejectsNullContentType() {
        assertThrows(InvalidImageException.class,
                () -> validator.validate(new byte[]{1}, null));
    }

    @Test
    void rejectsOversizedImage() {
        byte[] tooBig = new byte[(int) ImageValidator.MAX_BYTES + 1];
        assertThrows(InvalidImageException.class,
                () -> validator.validate(tooBig, "image/png"));
    }
}
