package org.example.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ApplicationService {
    private static final Set<String> STAFF_ROLES = Set.of("CONSULTANT", "ADMIN");
    private static final Set<String> STATUSES = Set.of("DRAFT", "SUBMITTED", "UNDER_REVIEW", "PROCESSING", "EMBASSY", "APPROVED", "REJECTED");
    private static final Map<String, Set<String>> STATUS_TRANSITIONS = Map.of(
            "SUBMITTED", Set.of("UNDER_REVIEW", "REJECTED"),
            "UNDER_REVIEW", Set.of("PROCESSING", "REJECTED"),
            "PROCESSING", Set.of("EMBASSY", "APPROVED", "REJECTED"),
            "EMBASSY", Set.of("PROCESSING", "APPROVED", "REJECTED"),
            "APPROVED", Set.of(),
            "REJECTED", Set.of(),
            "DRAFT", Set.of()
    );
    private final ApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final CatalogRepository catalogRepository;

    public ApplicationService(ApplicationRepository applicationRepository, AccountRepository accountRepository, CatalogRepository catalogRepository) {
        this.applicationRepository = applicationRepository;
        this.accountRepository = accountRepository;
        this.catalogRepository = catalogRepository;
    }

    @Transactional
    public Map<String, Object> create(ApplicationCreateRequest request) {
        Object value = request.get("visaRuleId");
        if (value == null) throw new IllegalArgumentException("visaRuleId is required.");
        long visaRuleId = Long.parseLong(String.valueOf(value));
        return create(visaRuleId);
    }

    @Transactional
    public Map<String, Object> create(long visaRuleId) {
        CustomUserDetails user = CurrentUser.get();
        if (!user.hasRole("USER")) throw new BusinessException("Only applicants can create visa applications.");
        Long applicantId = accountRepository.applicantId(user.getUserId());
        Long nationalityId = accountRepository.applicantNationality(user.getUserId());
        if (applicantId == null || nationalityId == null)
            throw new BusinessException("Applicant profile is incomplete.");
        Map<String, Object> rule = catalogRepository.catalogItem(visaRuleId);
        if (!"Y".equals(String.valueOf(rule.get("isActive"))))
            throw new BusinessException("This visa rule is not active.");
        long ruleNationality = ((Number) rule.get("nationalityCountryId")).longValue();
        if (ruleNationality != nationalityId)
            throw new BusinessException("This visa rule is not available for your nationality.");
        long applicationId = applicationRepository.createApplication(applicantId, visaRuleId);
        return Map.of("applicationId", applicationId, "currentStatus", "DRAFT");
    }

    public List<Map<String, Object>> list() {
        CustomUserDetails user = CurrentUser.get();
        if (user.hasRole("USER")) {
            Long applicantId = accountRepository.applicantId(user.getUserId());
            return applicantId == null ? List.of() : applicationRepository.listForApplicant(applicantId);
        }
        requireStaffRole(user);
        return applicationRepository.listAll(null, null);
    }

    public List<Map<String, Object>> searchStaff(String status, String search) {
        requireStaffRole(CurrentUser.get());
        if (status != null && !status.isBlank() && !STATUSES.contains(status.toUpperCase()))
            throw new IllegalArgumentException("Unknown application status.");
        return applicationRepository.listAll(status == null ? null : status.toUpperCase(), search);
    }

    public Map<String, Object> get(long applicationId) {
        ensureCanAccess(applicationId);
        return applicationRepository.getApplication(applicationId);
    }

    @Transactional
    public void updateDetails(long applicationId, ApplicationDetailsRequest body) {
        ensureApplicantOwns(applicationId);
        if (!"DRAFT".equals(applicationRepository.status(applicationId)))
            throw new BusinessException("Only draft applications can be edited.");
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("passportNumber", bounded(body, "passportNumber", 50));
        fields.put("passportIssueDate", dateText(body, "passportIssueDate"));
        fields.put("passportExpiryDate", dateText(body, "passportExpiryDate"));
        fields.put("dateOfBirth", dateText(body, "dateOfBirth"));
        fields.put("gender", bounded(body, "gender", 20));
        fields.put("maritalStatus", bounded(body, "maritalStatus", 30));
        fields.put("occupation", bounded(body, "occupation", 100));
        fields.put("residentialAddress", bounded(body, "residentialAddress", 500));
        fields.put("travelPurpose", bounded(body, "travelPurpose", 250));
        fields.put("expectedArrivalDate", dateText(body, "expectedArrivalDate"));
        fields.put("additionalInfo", bounded(body, "additionalInfo", 3900));
        LocalDate issue = fields.get("passportIssueDate") == null ? null : LocalDate.parse(String.valueOf(fields.get("passportIssueDate")));
        LocalDate expiry = fields.get("passportExpiryDate") == null ? null : LocalDate.parse(String.valueOf(fields.get("passportExpiryDate")));
        LocalDate dob = fields.get("dateOfBirth") == null ? null : LocalDate.parse(String.valueOf(fields.get("dateOfBirth")));
        LocalDate arrival = fields.get("expectedArrivalDate") == null ? null : LocalDate.parse(String.valueOf(fields.get("expectedArrivalDate")));
        if (issue != null && expiry != null && !expiry.isAfter(issue))
            throw new IllegalArgumentException("Passport expiry date must be later than issue date.");
        if (dob != null && dob.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Date of birth cannot be in the future.");
        if (arrival != null && arrival.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Expected arrival date cannot be in the past.");
        applicationRepository.saveDetails(applicationId, fields);
    }

    @Transactional
    public void submit(long applicationId) {
        ensureApplicantOwns(applicationId);
        if (!"DRAFT".equals(applicationRepository.status(applicationId)))
            throw new BusinessException("Only a DRAFT application can be submitted.");
        Map<String, Object> details = applicationRepository.submissionDetails(applicationId);
        for (String field : List.of("passportNumber", "passportIssueDate", "passportExpiryDate", "dateOfBirth", "residentialAddress", "travelPurpose")) {
            Object value = details.get(field);
            if (value == null || String.valueOf(value).isBlank())
                throw new BusinessException("Complete the required personal and passport information before submission.");
        }
        applicationRepository.submit(applicationId);
    }

    public List<Map<String, Object>> history(long applicationId) {
        ensureCanAccess(applicationId);
        return applicationRepository.history(applicationId);
    }

    @Transactional
    public void setStatus(long applicationId, ApplicationStatusRequest body) {
        CustomUserDetails user = CurrentUser.get();
        requireStaffRole(user);
        Long staffId = accountRepository.staffId(user.getUserId());
        if (staffId == null)
            throw new BusinessException("An active staff profile is required to update an application.");
        String status = RequestValues.required(body, "status", 30).toUpperCase();
        if (!STATUSES.contains(status) || status.equals("DRAFT") || status.equals("SUBMITTED"))
            throw new IllegalArgumentException("Staff may set only UNDER_REVIEW, PROCESSING, EMBASSY, APPROVED, or REJECTED.");
        String note = RequestValues.text(body, "note");
        if (note != null && note.length() > 1000) throw new IllegalArgumentException("Status note is too long.");
        String oldStatus = applicationRepository.status(applicationId);
        if (oldStatus.equals(status)) return;
        if (!STATUS_TRANSITIONS.getOrDefault(oldStatus, Set.of()).contains(status))
            throw new BusinessException("Status transition from " + oldStatus + " to " + status + " is not allowed.");
        if ("REJECTED".equals(status) && (note == null || note.isBlank()))
            throw new BusinessException("A status note is required when rejecting an application.");
        applicationRepository.changeStatus(applicationId, status, staffId, note);
    }

    public void ensureCanAccess(long applicationId) {
        CustomUserDetails user = CurrentUser.get();
        if (user.hasRole("USER")) ensureApplicantOwns(applicationId);
        else requireStaffRole(user);
    }

    public void ensureApplicantOwns(long applicationId) {
        Long applicantId = accountRepository.applicantId(CurrentUser.id());
        if (applicantId == null) throw new BusinessException("Applicant profile not found.");
        applicationRepository.assertOwned(applicationId, applicantId);
    }

    public long applicantIdForCurrentUser() {
        Long id = accountRepository.applicantId(CurrentUser.id());
        if (id == null) throw new BusinessException("Applicant profile not found.");
        return id;
    }

    public static void requireStaffRole(CustomUserDetails user) {
        if (user.getRoleNames().stream().noneMatch(STAFF_ROLES::contains))
            throw new org.springframework.security.access.AccessDeniedException("Staff access required.");
    }

    private String bounded(Map<String, Object> body, String key, int limit) {
        String v = RequestValues.text(body, key);
        if (v != null && v.length() > limit)
            throw new IllegalArgumentException(key + " must be at most " + limit + " characters.");
        return v;
    }

    private String dateText(Map<String, Object> body, String key) {
        LocalDate date = RequestValues.date(body, key);
        return date == null ? null : date.toString();
    }
}
