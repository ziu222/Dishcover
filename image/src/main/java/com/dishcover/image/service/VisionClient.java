package com.dishcover.image.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

/**
 * Lớp DUY NHẤT biết tới Spring AI/Vision API cụ thể (CLAUDE.md mục 9 "S"/"D") — gọi model đa
 * phương thức (gpt-4o-mini qua endpoint OpenAI, cấu hình application.yml) 1 lần với VISION_PROMPT
 * ép JSON, rồi parse khoan dung. Đổi provider vision sau này chỉ sửa trong đây.
 *
 * <p>Dùng {@code ChatClient.Builder} auto-config (đã trỏ OpenAI) — cùng cách {@code GeminiProvider}
 * bên RAG Service dựng ChatClient.</p>
 */
@Component
public class VisionClient {

    /** VISION_PROMPT — dùng NGUYÊN VĂN theo CLAUDE.md mục 7, không sửa chữ. */
    static final String VISION_PROMPT = """
            Bạn là hệ thống nhận diện nguyên liệu nấu ăn. Nhìn ảnh và liệt kê các
            NGUYÊN LIỆU NẤU ĂN nhìn thấy được.
            YÊU CẦU:
            - Trả về DUY NHẤT một JSON array, không markdown, không giải thích.
            - Mỗi phần tử: {"name": "<tên tiếng Việt phổ thông>", "confidence": <0..1>,
                            "quantity_guess": "<ước lượng, vd: 2 quả, null nếu không rõ>"}
            - Chỉ liệt kê thứ ăn được dùng để nấu. Bỏ qua bát đĩa, tay người, bao bì.
            - Nếu không chắc giữa 2 loại, chọn loại phổ biến hơn ở Việt Nam và giảm confidence.
            - Nếu ảnh không có nguyên liệu nào: trả về [].
            """;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChatClient chatClient;

    /**
     * @param builder {@code ChatClient.Builder} do Spring AI auto-config cung cấp (đã trỏ OpenAI
     *                qua endpoint tương thích, cấu hình ở application.yml)
     */
    public VisionClient(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * Gọi Vision API nhận diện nguyên liệu trong ảnh.
     *
     * @param image ảnh đã resize kèm mime-type (từ ImageResizer)
     * @return danh sách nguyên liệu thô (rỗng nếu ảnh không có nguyên liệu — model trả {@code []})
     * @throws IllegalStateException nếu phản hồi model không chứa JSON array hợp lệ
     */
    public List<RawRecognizedItem> recognize(ImageResizer.ResizedImage image) {
        MimeType mimeType = MimeTypeUtils.parseMimeType(image.mimeType());
        String raw = chatClient.prompt()
                .user(u -> u.text(VISION_PROMPT).media(mimeType, new ByteArrayResource(image.bytes())))
                .call()
                .content();
        return parseJsonArrayLenient(raw);
    }

    /**
     * Parse khoan dung: dù VISION_PROMPT cấm markdown/giải thích, model đa phương thức vẫn có xác
     * suất bọc {@code ```json} hoặc kèm câu chữ. Cắt lấy đúng đoạn {@code [...]} đầu-cuối rồi parse.
     *
     * @param raw chuỗi model trả về
     * @return danh sách item (có thể rỗng nếu là {@code []})
     * @throws IllegalStateException nếu không tìm thấy JSON array trong phản hồi
     */
    static List<RawRecognizedItem> parseJsonArrayLenient(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Vision API trả phản hồi rỗng");
        }
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start < 0 || end < 0 || end < start) {
            throw new IllegalStateException("Phản hồi Vision không chứa JSON array: " + raw);
        }
        String json = raw.substring(start, end + 1);
        try {
            return MAPPER.readValue(json, new TypeReference<List<RawRecognizedItem>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Không parse được JSON từ Vision: " + json, ex);
        }
    }
}
