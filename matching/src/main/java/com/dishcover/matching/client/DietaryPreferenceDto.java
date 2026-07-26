package com.dishcover.matching.client;

/** Map response của GET /users/me/dietary-preferences (user/dto/DietaryDtos.DietaryPreferenceResponse). */
public record DietaryPreferenceDto(Long id, String type, String value) {
}
