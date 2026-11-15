package org.example.service;

import org.example.repository.AdminRepository;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AdminService {
    private static final Set<String> STAFF_ROLES = Set.of("CONSULTANT", "ADMIN");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final AdminRepository adminRepository;
    private final AccountRepository accountRepository;
    private final CatalogRepository catalogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository, AccountRepository accountRepository, CatalogRepository catalogRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.accountRepository = accountRepository;
        this.catalogRepository = catalogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Map<String, Object>> allCountries() {
        return catalogRepository.countries(false);
    }

    public List<Map<String, Object>> allVisaTypes() {
        return catalogRepository.visaTypes(false);
    }

    public List<Map<String, Object>> visaRules() {
        return adminRepository.visaRules();
    }

    public List<Map<String, Object>> checklistItems() {
        return adminRepository.checklistItems();
    }

    public List<Map<String, Object>> universities() {
        return adminRepository.universities();
    }

    public List<Map<String, Object>> staff() {
        return accountRepository.listStaff();
    }

    @Transactional
    public long createCountry(CountryRequest b) {
        String name = RequestValues.required(b, "countryName", 100);
        String code = RequestValues.required(b, "destinationCode", 10).toUpperCase(Locale.ROOT);
        return adminRepository.createCountry(name, code, active(b));
    }

    @Transactional
    public void updateCountry(long id, CountryRequest b) {
        adminRepository.updateCountry(id, RequestValues.required(b, "countryName", 100), RequestValues.required(b, "destinationCode", 10).toUpperCase(Locale.ROOT), active(b));
    }

    @Transactional
    public long createVisaType(VisaTypeRequest b) {
        String desc = limit(RequestValues.text(b, "description"), 1000, "description");
        return adminRepository.createVisaType(RequestValues.required(b, "typeName", 100), desc, active(b));
    }

    @Transactional
    public void updateVisaType(long id, VisaTypeRequest b) {
        adminRepository.updateVisaType(id, RequestValues.required(b, "typeName", 100), limit(RequestValues.text(b, "description"), 1000, "description"), active(b));
    }

    @Transactional
    public long createVisaRule(VisaRuleRequest b) {
        long typeId = RequestValues.requiredLong(b, "visaTypeId");
        long destinationId = RequestValues.requiredLong(b, "destinationCountryId");
        long nationalityId = RequestValues.requiredLong(b, "nationalityCountryId");
        catalogRepository.assertActiveCountry(destinationId);
        catalogRepository.assertActiveCountry(nationalityId);
        catalogRepository.assertActiveVisaType(typeId);
        BigDecimal fee = RequestValues.decimal(b, "serviceFee");
        if (fee == null || fee.signum() < 0) throw new IllegalArgumentException("serviceFee must be zero or greater.");
        String currency = currency(RequestValues.required(b, "currencyCode", 3));
        String processing = limit(RequestValues.text(b, "processingTime"), 100, "processingTime");
        return adminRepository.createVisaRule(typeId, destinationId, nationalityId, fee, currency, processing,
                active(b), CurrentUser.id(), limit(RequestValues.text(b, "updateNote"), 1000, "updateNote"));
    }

    @Transactional
    public void updateVisaRule(long id, VisaRuleRequest b) {
        long typeId = RequestValues.requiredLong(b, "visaTypeId");
        long destinationId = RequestValues.requiredLong(b, "destinationCountryId");
        long nationalityId = RequestValues.requiredLong(b, "nationalityCountryId");
        catalogRepository.assertActiveCountry(destinationId);
        catalogRepository.assertActiveCountry(nationalityId);
        catalogRepository.assertActiveVisaType(typeId);
        adminRepository.updateVisaRule(id, typeId, destinationId, nationalityId,
                limit(RequestValues.text(b, "processingTime"), 100, "processingTime"), active(b), CurrentUser.id(),
                limit(RequestValues.text(b, "updateNote"), 1000, "updateNote"));
        // Fee changes must go through the procedure so the audit trail is not skipped.
        if (b.containsKey("serviceFee") || b.containsKey("currencyCode")) {
            BigDecimal fee = RequestValues.decimal(b, "serviceFee");
            if (fee == null || fee.signum() < 0) throw new IllegalArgumentException("serviceFee must be non-negative.");
            adminRepository.updateFee(id, fee, currency(RequestValues.required(b, "currencyCode", 3)),
                    CurrentUser.id(), limit(RequestValues.text(b, "updateNote"), 1000, "updateNote"));
        }
    }

    @Transactional
    public void updateRuleActive(long id, VisaRuleActivationRequest b) {
        adminRepository.updateVisaRuleActive(id, active(b), CurrentUser.id(), limit(RequestValues.text(b, "updateNote"), 1000, "updateNote"));
    }

    @Transactional
    public void updateFee(long id, VisaRuleFeeRequest b) {
        BigDecimal fee = RequestValues.decimal(b, "serviceFee");
        if (fee == null || fee.signum() < 0) throw new IllegalArgumentException("serviceFee must be zero or greater.");
        String currency = currency(RequestValues.required(b, "currencyCode", 3));
        adminRepository.updateFee(id, fee, currency, CurrentUser.id(), limit(RequestValues.text(b, "changeNote"), 1000, "changeNote"));
    }

    public List<Map<String, Object>> feeHistory(long id) {
        return adminRepository.feeHistory(id);
    }

    @Transactional
    public long createChecklistItem(ChecklistItemRequest b) {
        return adminRepository.createChecklistItem(RequestValues.required(b, "itemName", 200),
                limit(RequestValues.text(b, "itemDescription"), 1000, "itemDescription"), active(b));
    }

    @Transactional
    public void updateChecklistItem(long id, ChecklistItemRequest b) {
        adminRepository.updateChecklistItem(id, RequestValues.required(b, "itemName", 200),
                limit(RequestValues.text(b, "itemDescription"), 1000, "itemDescription"), active(b));
    }

    @Transactional
    public void attachChecklist(long ruleId, VisaRuleChecklistRequest b) {
        long itemId = RequestValues.requiredLong(b, "checklistItemId");
        String required = RequestValues.bool(b, "required", true) ? "Y" : "N";
        Long orderValue = RequestValues.longValue(b, "displayOrder");
        int order = orderValue == null ? 1 : Math.toIntExact(orderValue);
        if (order < 1 || order > 9999) throw new IllegalArgumentException("displayOrder must be between 1 and 9999.");
        adminRepository.attachChecklist(ruleId, itemId, required, order, limit(RequestValues.text(b, "instructions"), 1000, "instructions"));
    }

    @Transactional
    public void detachChecklist(long ruleId, long itemId) {
        adminRepository.detachChecklist(ruleId, itemId);
    }

    @Transactional
    public Map<String, Object> createStaff(StaffRequest b) {
        String fullName = RequestValues.required(b, "fullName", 150);
        String username = RequestValues.required(b, "username", 80);
        String email = RequestValues.required(b, "email", 254).toLowerCase(Locale.ROOT);
        String password = RequestValues.required(b, "password", 200);
        String role = RequestValues.required(b, "role", 40).toUpperCase(Locale.ROOT);
        if (!STAFF_ROLES.contains(role)) throw new IllegalArgumentException("Staff role must be CONSULTANT or ADMIN.");
        if (password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Staff password must be at least 8 characters and no more than 72 UTF-8 bytes for BCrypt.");
        if (!username.matches("[A-Za-z0-9._-]{4,80}"))
            throw new IllegalArgumentException("Username must be 4 to 80 allowed characters.");
        if (!EMAIL.matcher(email).matches()) throw new IllegalArgumentException("Enter a valid email address.");
        long userId = accountRepository.createUser(fullName, username, passwordEncoder.encode(password), email, "ACTIVE");
        long staffId = accountRepository.createStaffProfile(userId, limit(RequestValues.text(b, "staffTitle"), 100, "staffTitle"), "Y");
        accountRepository.assignRole(userId, role, CurrentUser.id());
        return Map.of("userId", userId, "staffId", staffId, "role", role);
    }

    @Transactional
    public void updateStaff(long userId, StaffRequest b) {
        String status = RequestValues.required(b, "accountStatus", 20).toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "INACTIVE", "LOCKED", "PENDING").contains(status))
            throw new IllegalArgumentException("Invalid account status.");
        String active = RequestValues.bool(b, "isActive", true) ? "Y" : "N";
        String email = RequestValues.required(b, "email", 254).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) throw new IllegalArgumentException("Enter a valid email address.");
        if (!accountRepository.isStaffUser(userId))
            throw new com.nahidglobal.exception.ResourceNotFoundException("Staff profile not found.");
        if ("N".equals(active) && "ACTIVE".equals(status)) status = "INACTIVE";
        String role = RequestValues.text(b, "role");
        if (role != null && !role.isBlank()) {
            role = role.toUpperCase(Locale.ROOT);
            if (!STAFF_ROLES.contains(role)) throw new IllegalArgumentException("Role must be CONSULTANT or ADMIN.");
        }
        if (userId == CurrentUser.id() && (!"ACTIVE".equals(status) || "N".equals(active)
                || (role != null && !"ADMIN".equals(role)))) {
            throw new BusinessException("You cannot disable or remove your own active administrator access.");
        }
        accountRepository.updateStaffProfile(userId, RequestValues.required(b, "fullName", 150), email,
                status, limit(RequestValues.text(b, "staffTitle"), 100, "staffTitle"), active);
        if (role != null && !role.isBlank()) accountRepository.replaceRoles(userId, List.of(role), CurrentUser.id());
    }

    @Transactional
    public void changeStaffRoles(long userId, StaffRolesRequest b) {
        if (!accountRepository.isStaffUser(userId))
            throw new com.nahidglobal.exception.ResourceNotFoundException("Staff profile not found.");
        List<Object> raw = RequestValues.list(b, "roles");
        if (raw.size() != 1) throw new IllegalArgumentException("Exactly one role must be assigned per account.");
        List<String> roles = raw.stream().map(String::valueOf).map(s -> s.toUpperCase(Locale.ROOT)).distinct().toList();
        if (roles.size() != 1 || !STAFF_ROLES.containsAll(roles))
            throw new IllegalArgumentException("Only CONSULTANT and ADMIN roles may be assigned here.");
        if (userId == CurrentUser.id() && !roles.contains("ADMIN"))
            throw new BusinessException("You cannot remove your own administrator role.");
        accountRepository.replaceRoles(userId, roles, CurrentUser.id());
    }

    @Transactional
    public long createUniversity(UniversityRequest b) {
        long countryId = RequestValues.requiredLong(b, "countryId");
        catalogRepository.assertActiveCountry(countryId);
        return adminRepository.createUniversity(countryId, RequestValues.required(b, "universityName", 200),
                limit(RequestValues.text(b, "website"), 500, "website"), limit(RequestValues.text(b, "entryCriteria"), 3900, "entryCriteria"));
    }

    @Transactional
    public void updateUniversity(long id, UniversityRequest b) {
        long countryId = RequestValues.requiredLong(b, "countryId");
        catalogRepository.assertActiveCountry(countryId);
        adminRepository.updateUniversity(id, countryId, RequestValues.required(b, "universityName", 200),
                limit(RequestValues.text(b, "website"), 500, "website"), limit(RequestValues.text(b, "entryCriteria"), 3900, "entryCriteria"));
    }

    public Map<String, Object> report() {
        return adminRepository.dashboardReport();
    }

    private String active(Map<String, Object> body) {
        return RequestValues.bool(body, "isActive", true) ? "Y" : "N";
    }

    private String currency(String value) {
        String v = value.trim().toUpperCase(Locale.ROOT);
        if (!v.matches("[A-Z]{3}"))
            throw new IllegalArgumentException("currencyCode must contain exactly three letters.");
        return v;
    }

    private String limit(String value, int max, String field) {
        if (value != null && value.length() > max)
            throw new IllegalArgumentException(field + " must be at most " + max + " characters.");
        return value;
    }
}

