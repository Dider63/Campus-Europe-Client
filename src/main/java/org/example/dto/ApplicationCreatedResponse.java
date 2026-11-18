package org.example.dto;

import java.util.Map;

/** Same two JSON properties as the original POST /api/applications response. */
public record ApplicationCreatedResponse(long applicationId, String currentStatus) {
    public static ApplicationCreatedResponse from(Map<String, Object> value) {
        return new ApplicationCreatedResponse(((Number) value.get("applicationId")).longValue(),
                (String) value.get("currentStatus"));
    }
}
