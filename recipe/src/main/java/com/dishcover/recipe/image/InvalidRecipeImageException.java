package com.dishcover.recipe.image;

/** Ảnh upload không hợp lệ — map ra HTTP 422 ở GlobalExceptionHandler của Recipe Service. */
public class InvalidRecipeImageException extends RuntimeException {
    public InvalidRecipeImageException(String message) {
        super(message);
    }
}
