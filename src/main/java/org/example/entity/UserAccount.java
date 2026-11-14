package org.example.entity;


import java.time.LocalDateTime;

public record UserAccount(
        Long userId,
        String fullName,
        String username,
        String passwordHash,
        String email,
        String accountStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}