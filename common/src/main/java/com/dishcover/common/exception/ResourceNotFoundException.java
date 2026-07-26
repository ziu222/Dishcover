package com.dishcover.common.exception;

/** 404 — không tìm thấy tài nguyên. Dùng chung nhiều service (User/Inventory/Recipe). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
