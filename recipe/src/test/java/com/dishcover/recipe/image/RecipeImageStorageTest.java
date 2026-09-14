package com.dishcover.recipe.image;

import com.dishcover.common.image.ImageResizer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Kiểm tra phần quyết định của storage: chặn ảnh không hợp lệ TRƯỚC khi gọi S3, và sinh key/URL
 * đúng dạng. Không gọi S3 thật — chỉ cần biết đã gửi đúng bucket/key/content-type.
 */
class RecipeImageStorageTest {

    private final S3Client s3 = mock(S3Client.class);
    private final RecipeImageStorage storage =
            new RecipeImageStorage(s3, new ImageResizer(), "bucket-test",
                    "https://www.example.com/", "dev/");

    private static byte[] pngBytes() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    @Test
    void anhRongBiTuChoiVaKhongGoiS3() {
        assertThatThrownBy(() -> storage.store("r1", new byte[0], "image/png"))
                .isInstanceOf(InvalidRecipeImageException.class);
        verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void saiDinhDangBiTuChoiVaKhongGoiS3() throws Exception {
        assertThatThrownBy(() -> storage.store("r1", pngBytes(), "application/pdf"))
                .isInstanceOf(InvalidRecipeImageException.class);
        verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void anhVuot5MbBiTuChoi() {
        byte[] qua = new byte[(int) RecipeImageStorage.MAX_BYTES + 1];
        assertThatThrownBy(() -> storage.store("r1", qua, "image/png"))
                .isInstanceOf(InvalidRecipeImageException.class)
                .hasMessageContaining("5MB");
        verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void anhHopLeDuocGhiLenS3VaTraVeUrlCongKhai() throws Exception {
        String url = storage.store("recipe_001", pngBytes(), "image/png");

        var captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(captor.capture(), any(RequestBody.class));
        PutObjectRequest req = captor.getValue();

        assertThat(req.bucket()).isEqualTo("bucket-test");
        // key-prefix cho môi trường dev + prefix cố định + id công thức để truy vết
        assertThat(req.key()).startsWith("dev/recipe-images/recipe_001-");
        // URL trả về phải ghép được từ base-url (đã cắt dấu / thừa) + key
        assertThat(url).isEqualTo("https://www.example.com/" + req.key());
    }

    @Test
    void moiLanUploadSinhKeyKhacNhauNenKhongPhaiInvalidateCache() throws Exception {
        String first = storage.store("recipe_001", pngBytes(), "image/png");
        String second = storage.store("recipe_001", pngBytes(), "image/png");
        assertThat(first).isNotEqualTo(second);
    }
}
