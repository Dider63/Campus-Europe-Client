package org.example.dto;

import java.util.Map;

public record RegisterResponse(long userId, long applicantId, String username, String role) {
    public static RegisterResponse from(Map<String, Object> value) {
        return new RegisterResponse(((Number) value.get("userId")).longValue(),
                ((Number) value.get("applicantId")).longValue(),
                (String) value.get("username"), (String) value.get("role"));
    }
}
