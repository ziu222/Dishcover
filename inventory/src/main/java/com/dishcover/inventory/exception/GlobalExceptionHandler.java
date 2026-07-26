package com.dishcover.inventory.exception;

import com.dishcover.common.exception.CommonExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Không có exception riêng — toàn bộ xử lý lỗi kế thừa từ CommonExceptionHandler (common). */
@RestControllerAdvice
public class GlobalExceptionHandler extends CommonExceptionHandler {
}
