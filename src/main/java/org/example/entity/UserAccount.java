package org.example.entity;

public record UserAccount(
        long userId,
        String username,
        String passwordHash,
        String email,
        String accountStatus,
){ }
