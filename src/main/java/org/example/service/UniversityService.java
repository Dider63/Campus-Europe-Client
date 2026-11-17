package org.example.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UniversityService {
    private final UniversityRepository universityRepository;
    private final AccountRepository accountRepository;
    public UniversityService(UniversityRepository universityRepository, AccountRepository accountRepository) { this.universityRepository = universityRepository; this.accountRepository = accountRepository; }

    public List<Map<String, Object>> academicRecords() {
        return universityRepository.academicRecords(requireApplicant());
    }

    @Transactional
    public Map<String, Object> createAcademicRecord(AcademicRecordRequest body) {
        long applicantId = requireApplicant();
        String qualification = RequestValues.required(body, "qualification", 150);
        String result = RequestValues.text(body, "result");
        if (result != null && result.length() > 100) throw new IllegalArgumentException("Result must be at most 100 characters.");
        Long yearValue = RequestValues.longValue(body, "graduationYear");
        Integer year = yearValue == null ? null : Math.toIntExact(yearValue);
        if (year != null && (year < 1950 || year > 2100)) throw new IllegalArgumentException("Graduation year must be between 1950 and 2100.");
        long id = universityRepository.createAcademicRecord(applicantId, qualification, result, year);
        return Map.of("academicRecordId", id);
    }

    @Transactional
    public void updateAcademicRecord(long id, AcademicRecordRequest body) {
        long applicantId = requireApplicant();
        String qualification = RequestValues.required(body, "qualification", 150);
        String result = RequestValues.text(body, "result");
        Long yearValue = RequestValues.longValue(body, "graduationYear");
        Integer year = yearValue == null ? null : Math.toIntExact(yearValue);
        if (year != null && (year < 1950 || year > 2100)) throw new IllegalArgumentException("Graduation year must be between 1950 and 2100.");
        universityRepository.updateAcademicRecord(id, applicantId, qualification, result, year);
    }

    public List<Map<String, Object>> academicRecordsForStaff(long applicantId) {
        ApplicationService.requireStaffRole(CurrentUser.get());
        return universityRepository.academicRecords(applicantId);
    }

    public List<Map<String, Object>> universities(Long countryId) { return universityRepository.universities(countryId); }

    public List<Map<String, Object>> recommendations(Long applicantFilter) {
        if (CurrentUser.hasRole("USER")) return universityRepository.recommendations(requireApplicant());
        ApplicationService.requireStaffRole(CurrentUser.get());
        return universityRepository.recommendations(applicantFilter);
    }

    @Transactional
    public Map<String, Object> createRecommendation(UniversityRecommendationRequest body) {
        ApplicationService.requireStaffRole(CurrentUser.get());
        Long applicantId = RequestValues.longValue(body, "applicantId");
        long academicId = RequestValues.requiredLong(body, "academicRecordId");
        if (applicantId == null || applicantId <= 0) throw new IllegalArgumentException("applicantId is required for staff-created recommendations.");
        if (!universityRepository.academicRecordBelongs(academicId, applicantId)) throw new BusinessException("The academic record does not belong to the selected applicant.");
        String note = RequestValues.text(body, "recommendationNote");
        if (note != null && note.length() > 1000) throw new IllegalArgumentException("Recommendation note is too long.");
        long recommendationId = universityRepository.createRecommendation(applicantId, academicId, note);
        List<Object> ids = RequestValues.list(body, "universities");
        List<Long> added = new ArrayList<>();
        for (Object item : ids) {
            if (!(item instanceof Map<?, ?> map)) throw new IllegalArgumentException("Each university recommendation must be an object.");
            Object rawId = map.get("universityId");
            if (rawId == null) throw new IllegalArgumentException("universityId is required.");
            long universityId = Long.parseLong(String.valueOf(rawId));
            Object rankValue = map.get("rankNo");
            Integer rank = rankValue == null || String.valueOf(rankValue).isBlank() ? null : Integer.valueOf(String.valueOf(rankValue));
            if (rank != null && rank < 1) throw new IllegalArgumentException("University rank must be at least 1.");
            String matchNote = map.get("matchNote") == null ? null : String.valueOf(map.get("matchNote"));
            if (matchNote != null && matchNote.length() > 1000) throw new IllegalArgumentException("University match note is too long.");
            universityRepository.addRecommendedUniversity(recommendationId, universityId, rank, matchNote);
            added.add(universityId);
        }
        return Map.of("recommendationId", recommendationId, "universitiesAdded", added,
                "disclaimer", "Recommendations are staff-assisted suggestions, not admission guarantees or official eligibility decisions.");
    }

    private long requireApplicant() {
        if (!CurrentUser.hasRole("USER")) throw new org.springframework.security.access.AccessDeniedException("Applicant access required.");
        Long applicantId = accountRepository.applicantId(CurrentUser.id());
        if (applicantId == null) throw new BusinessException("Applicant profile not found.");
        return applicantId;
    }
}

