package com.dishcover.image.controller;

import com.dishcover.image.dto.ImageDtos.RecognizeResponse;
import com.dishcover.image.exception.InvalidImageException;
import com.dishcover.image.service.RecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Nhận diện nguyên liệu từ ảnh (AI #2, CLAUDE.md mục 7). Yêu cầu JWT hợp lệ (SecurityConfig).
 * KHÔNG ghi DB: client hiển thị màn xác nhận rồi tự POST sang Inventory Service (human-in-the-loop).
 */
@RestController
@RequestMapping("/recognize")
public class ImageRecognitionController {

    private final RecognitionService service;

    public ImageRecognitionController(RecognitionService service) {
        this.service = service;
    }

    @Operation(summary = "Nhận diện nguyên liệu từ ảnh (đề xuất, chưa ghi DB — cần người dùng xác nhận)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecognizeResponse recognize(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Thiếu file ảnh (tham số 'file')");
        }
        try {
            return service.recognize(file.getBytes(), file.getContentType());
        } catch (IOException ex) {
            throw new InvalidImageException("Không đọc được nội dung file ảnh");
        }
    }
}
