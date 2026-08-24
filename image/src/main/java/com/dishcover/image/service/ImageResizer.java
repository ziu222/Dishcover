package com.dishcover.image.service;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Resize ảnh về cạnh dài ≤1024px để giảm token/chi phí gọi Vision API (CLAUDE.md mục 7 "Kiểm soát
 * chi phí"). Đây là tối ưu chi phí, KHÔNG phải bước bắt buộc cho tính đúng — nên mọi trường hợp
 * không xử lý được đều pass-through nguyên bản (đã được ImageValidator chặn ≤5MB, Vision API nhận
 * trực tiếp).
 */
@Component
public class ImageResizer {

    static final int MAX_DIMENSION = 1024;

    /** Ảnh sau resize kèm mime-type tương ứng (đã re-encode JPEG thì đổi mime, pass-through thì giữ). */
    public record ResizedImage(byte[] bytes, String mimeType) {
    }

    /**
     * @param bytes               ảnh gốc (đã qua ImageValidator)
     * @param originalContentType content-type gốc, dùng khi pass-through
     * @return ảnh đã resize (mime image/jpeg) nếu decode được và lớn hơn 1024px; ngược lại nguyên bản
     */
    public ResizedImage resize(byte[] bytes, String originalContentType) {
        BufferedImage img = decode(bytes);
        // ponytail: ImageIO không decode được webp (không có plugin) -> pass-through, Vision API vẫn
        // nhận webp. Thêm thư viện webp chỉ để resize là không đáng ở quy mô này (đã cap 5MB).
        if (img == null) {
            return new ResizedImage(bytes, originalContentType);
        }
        int longest = Math.max(img.getWidth(), img.getHeight());
        if (longest <= MAX_DIMENSION) {
            return new ResizedImage(bytes, originalContentType);
        }
        double scale = (double) MAX_DIMENSION / longest;
        int newW = Math.max(1, (int) Math.round(img.getWidth() * scale));
        int newH = Math.max(1, (int) Math.round(img.getHeight() * scale));

        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newW, newH, null);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(scaled, "jpeg", out);
        } catch (IOException ex) {
            return new ResizedImage(bytes, originalContentType);
        }
        return new ResizedImage(out.toByteArray(), "image/jpeg");
    }

    private BufferedImage decode(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException ex) {
            return null;
        }
    }
}
