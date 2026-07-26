package com.dishcover.matching.exception;

import com.dishcover.common.exception.CommonExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Không có exception riêng — toàn bộ xử lý lỗi (kể cả PlanRequiredException) kế thừa từ CommonExceptionHandler (common). */
@RestControllerAdvice
public class GlobalExceptionHandler extends CommonExceptionHandler {
}
