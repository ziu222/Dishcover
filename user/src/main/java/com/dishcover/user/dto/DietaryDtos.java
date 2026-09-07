package com.dishcover.user.dto;

import com.dishcover.user.entity.DietaryPreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO cho hồ sơ ăn uống (dị ứng / chế độ ăn) — Matching Service đọc để lọc dị ứng. */
public final class DietaryDtos {

    private DietaryDtos() {
    }

    /**
     * Yêu cầu thêm một mục hồ sơ ăn uống.
     *
     * @param type  ALLERGY, DIET hoặc TAG_PREFERENCE (định hướng ăn uống — VD "high protein",
     *              dùng bởi Matching Service TagPreferenceRule, xem docs/specs/diet-direction-recommendation.md)
     * @param value giá trị cụ thể, VD 'hải sản', 'chay', 'high protein'
     */
    public record DietaryPreferenceRequest(
            @NotBlank @Pattern(regexp = "ALLERGY|DIET|TAG_PREFERENCE",
                    message = "type phải là ALLERGY, DIET hoặc TAG_PREFERENCE") String type,
            @NotBlank @Size(max = 50) String value
    ) {
    }

    /**
     * Thông tin một mục hồ sơ ăn uống trả ra API.
     *
     * @param type  ALLERGY, DIET hoặc TAG_PREFERENCE
     * @param value giá trị cụ thể, VD 'hải sản', 'chay', 'high protein'
     */
    public record DietaryPreferenceResponse(
            Long id,
            String type,
            String value
    ) {
        /**
         * Chuyển entity {@link DietaryPreference} sang DTO trả ra API.
         *
         * @param p entity nguồn
         * @return DTO tương ứng
         */
        public static DietaryPreferenceResponse from(DietaryPreference p) {
            return new DietaryPreferenceResponse(p.getId(), p.getType(), p.getValue());
        }
    }
}
