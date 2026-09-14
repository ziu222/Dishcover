package com.dishcover.recipe.image;

import com.dishcover.common.image.ImageResizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Ghi ảnh công thức lên S3 và trả về URL công khai.
 *
 * Bucket để riêng tư, người dùng đọc ảnh qua CloudFront (xem {@code infra/aws/recipe-images.tf}) —
 * cùng distribution với frontend nên ảnh cùng origin với trang, không dính CORS. Ghi bằng IAM task
 * role của ECS, KHÔNG có access key nào trong cấu hình (CLAUDE.md mục 9, "secret càng ít càng tốt").
 *
 * Cố ý không dùng {@code ImageValidator} của module image: nó kéo theo cả exception riêng của module
 * đó, trong khi phần kiểm tra thật sự chỉ là 3 điều kiện dưới đây. {@link ImageResizer} thì dùng lại
 * vì đó mới là phần có logic đáng kể (đã chuyển sang common).
 */
@Component
public class RecipeImageStorage {

    /** Giống ImageValidator bên Image Service — cùng ngưỡng để trải nghiệm nhất quán. */
    static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String PREFIX = "recipe-images/";

    private final S3Client s3;
    private final ImageResizer resizer;
    private final String bucket;
    private final String publicBaseUrl;
    private final String keyPrefix;

    public RecipeImageStorage(S3Client s3, ImageResizer resizer,
                              @Value("${app.recipe-images.bucket}") String bucket,
                              @Value("${app.recipe-images.public-base-url}") String publicBaseUrl,
                              @Value("${app.recipe-images.key-prefix:}") String keyPrefix) {
        this.s3 = s3;
        this.resizer = resizer;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        this.keyPrefix = keyPrefix;
    }

    /**
     * Kiểm tra, resize rồi ghi ảnh lên S3.
     *
     * @param recipeId    id công thức, chỉ dùng để đặt tên file cho dễ truy vết
     * @param bytes       nội dung ảnh
     * @param contentType content-type client gửi lên
     * @return URL công khai của ảnh vừa ghi
     * @throws InvalidRecipeImageException nếu ảnh rỗng, quá 5MB hoặc sai định dạng
     */
    public String store(String recipeId, byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new InvalidRecipeImageException("Ảnh rỗng hoặc không đọc được");
        }
        if (bytes.length > MAX_BYTES) {
            throw new InvalidRecipeImageException("Ảnh vượt quá 5MB");
        }
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
        if (!ALLOWED.contains(normalized)) {
            throw new InvalidRecipeImageException("Chỉ nhận ảnh jpg, png hoặc webp");
        }

        ImageResizer.ResizedImage resized = resizer.resize(bytes, normalized);
        // Key có phần ngẫu nhiên nên mỗi lần đổi ảnh là một URL mới — không phải invalidate
        // CloudFront, và cũng không ghi đè mất ảnh cũ nếu có chỗ nào còn tham chiếu.
        String key = keyPrefix + PREFIX + recipeId + "-" + UUID.randomUUID() + extensionFor(resized.mimeType());

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(resized.mimeType())
                        .build(),
                RequestBody.fromBytes(resized.bytes()));

        return publicBaseUrl + "/" + key;
    }

    private static String extensionFor(String mimeType) {
        return switch (mimeType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
