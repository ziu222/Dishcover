package com.dishcover.recipe.exception;

import com.dishcover.common.exception.ApiError;
import com.dishcover.common.exception.CommonExceptionHandler;
import com.dishcover.recipe.image.InvalidRecipeImageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Kế thừa toàn bộ xử lý lỗi chung, thêm đúng một lỗi riêng của Recipe Service. */
@RestControllerAdvice
public class GlobalExceptionHandler extends CommonExceptionHandler {

    /** Ảnh upload sai định dạng/quá lớn — lỗi dữ liệu người dùng gửi, không phải lỗi hệ thống. */
    @ExceptionHandler(InvalidRecipeImageException.class)
    public ResponseEntity<ApiError> handleInvalidImage(InvalidRecipeImageException ex) {
        // Dung lai build() cua lop cha de co traceId nhat quan voi moi loi khac trong he thong.
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_IMAGE", ex.getMessage());
    }
}
