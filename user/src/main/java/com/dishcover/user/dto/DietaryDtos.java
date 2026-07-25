package com.dishcover.user.dto;

import com.dishcover.user.entity.DietaryPreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO cho hồ sơ ăn uống (dị ứng / chế độ ăn) — Matching Service đọc để lọc dị ứng. */
public final class DietaryDtos {

    private DietaryDtos() {
    }

    public record DietaryPreferenceRequest(
            @NotBlank @Pattern(regexp = "ALLERGY|DIET", message = "type phải là ALLERGY hoặc DIET") String type,
            @NotBlank @Size(max = 50) String value
    ) {
    }

    public record DietaryPreferenceResponse(
            Long id,
            String type,
            String value
    ) {
        public static DietaryPreferenceResponse from(DietaryPreference p) {
            return new DietaryPreferenceResponse(p.getId(), p.getType(), p.getValue());
        }
    }
}
