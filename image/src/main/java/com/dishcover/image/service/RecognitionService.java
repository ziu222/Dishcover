package com.dishcover.image.service;

import com.dishcover.image.dto.ImageDtos.RecognizeResponse;
import com.dishcover.image.exception.VisionUnavailableException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ghép 4 bước nhận diện (CLAUDE.md mục 7): validate → resize → gọi Vision (bọc resilience) →
 * chuẩn hóa. KHÔNG ghi DB — trả kết quả cho client hiển thị màn xác nhận (human-in-the-loop).
 */
@Service
public class RecognitionService {

    private static final long CALL_TIMEOUT_SECONDS = 20; // đệm hơn 15s TimeLimiter (giống RAG LlmGateway)

    private final ImageValidator validator;
    private final ImageResizer resizer;
    private final ResilientVisionCaller visionCaller;
    private final ResultNormalizer normalizer;

    public RecognitionService(ImageValidator validator, ImageResizer resizer,
                              ResilientVisionCaller visionCaller, ResultNormalizer normalizer) {
        this.validator = validator;
        this.resizer = resizer;
        this.visionCaller = visionCaller;
        this.normalizer = normalizer;
    }

    /**
     * @param bytes       nội dung ảnh
     * @param contentType content-type client gửi
     * @return danh sách nguyên liệu nhận diện đã chuẩn hóa (rỗng nếu ảnh không có nguyên liệu)
     * @throws com.dishcover.image.exception.InvalidImageException ảnh sai định dạng/dung lượng (422)
     * @throws VisionUnavailableException Vision API lỗi/timeout/mạch ngắt (503)
     */
    public RecognizeResponse recognize(byte[] bytes, String contentType) {
        validator.validate(bytes, contentType);
        ImageResizer.ResizedImage resized = resizer.resize(bytes, contentType);

        List<RawRecognizedItem> rawItems;
        try {
            rawItems = visionCaller.recognizeAsync(resized).get(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new VisionUnavailableException(
                    "Nhận diện ảnh tạm thời không khả dụng, bạn có thể nhập tay", ex);
        }
        return new RecognizeResponse(normalizer.normalize(rawItems, LocalDate.now()));
    }
}
