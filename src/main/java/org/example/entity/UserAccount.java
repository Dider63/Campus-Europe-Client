package org.example.entity;

/**
 * Immutable JDBC entity projection of the existing Oracle USER_ACCOUNT table.
 * This is a plain Java record; it does not use JPA or an ORM.
 */
public record UserAccount(
        long userId,
        String username,
        String passwordHash,
        String fullName,
        String email,
        String accountStatus
) { }
