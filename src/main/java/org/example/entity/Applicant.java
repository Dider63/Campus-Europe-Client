package org.example.entity;

import java.time.LocalDateTime;

public record Applicant(
        Long applicantId,
        Long userId,
        String phone,
        Long nationalityCountryId,
        LocalDateTime createdAt
) {
}
