package org.example.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ProfileService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final AccountRepository accountRepository;
    private final CatalogRepository catalogRepository;
    public ProfileService(AccountRepository accountRepository, CatalogRepository catalogRepository) { this.accountRepository = accountRepository; this.catalogRepository = catalogRepository; }
    public Map<String, Object> get() { return accountRepository.profile(CurrentUser.id()); }

    @Transactional
    public Map<String, Object> update(ProfileUpdateRequest body) {
        long userId = CurrentUser.id();
        String name = RequestValues.required(body, "fullName", 150);
        String email = RequestValues.required(body, "email", 254).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) throw new IllegalArgumentException("Enter a valid email address.");
        String phone = RequestValues.text(body, "phone");
        if (phone != null && phone.length() > 30) throw new IllegalArgumentException("Phone must be at most 30 characters.");
        accountRepository.updateBasicProfile(userId, name, email);
        if (CurrentUser.hasRole("USER")) {
            Long nationality = RequestValues.longValue(body, "nationalityCountryId");
            if (nationality == null) nationality = accountRepository.applicantNationality(userId);
            catalogRepository.assertActiveCountry(nationality);
            accountRepository.updateApplicantProfile(userId, phone, nationality);
        }
        return accountRepository.profile(userId);
    }
}