package org.example.service;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final AccountRepository accountRepository;
    private final CatalogRepository catalogRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AccountRepository accountRepository, CatalogRepository catalogRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.catalogRepository = catalogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest body) {
        String fullName = RequestValues.required(body, "fullName", 150);
        String username = RequestValues.required(body, "username", 80);
        String email = RequestValues.required(body, "email", 254).toLowerCase(Locale.ROOT);
        String password = RequestValues.required(body, "password", 200);
        long nationalityId = RequestValues.requiredLong(body, "nationalityCountryId");
        String phone = RequestValues.text(body, "phone");
        if (!EMAIL.matcher(email).matches()) throw new IllegalArgumentException("Enter a valid email address.");
        if (username.length() < 4 || !username.matches("[A-Za-z0-9._-]+"))
            throw new IllegalArgumentException("Username must be at least 4 characters and contain only letters, numbers, dot, underscore, or hyphen.");
        validatePassword(password);
        if (phone != null && phone.length() > 30)
            throw new IllegalArgumentException("Phone must be at most 30 characters.");
        // Validate the role before inserting anything, so an unmigrated schema returns
        // an actionable message rather than an opaque HTTP 500. Transaction still
        // protects against partial registration if any later insert fails.
        accountRepository.roleId("USER");
        catalogRepository.assertActiveCountry(nationalityId);
        long userId = accountRepository.createUser(fullName, username, passwordEncoder.encode(password), email, "ACTIVE");
        long applicantId = accountRepository.createApplicant(userId, phone, nationalityId);
        accountRepository.assignRole(userId, "USER", null);
        return Map.of("userId", userId, "applicantId", applicantId, "username", username, "role", "USER");
    }

    @Transactional
    public void changePassword(ChangePasswordRequest body) {
        String currentPassword = RequestValues.required(body, "currentPassword", 200);
        String newPassword = RequestValues.required(body, "newPassword", 200);
        String confirmPassword = RequestValues.required(body, "confirmPassword", 200);
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }
        validatePassword(newPassword);
        var user = CurrentUser.get();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Choose a password different from your current password.");
        }
        accountRepository.updatePasswordHash(user.getUserId(), passwordEncoder.encode(newPassword));
    }

    private static void validatePassword(String password) {
        if (password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at least 8 characters and no more than 72 UTF-8 bytes for BCrypt.");
        }
    }
}

