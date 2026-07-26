package com.dishcover.recipe.exception;

public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 404 — không tìm thấy công thức. */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
