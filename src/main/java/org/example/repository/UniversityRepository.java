package org.example.repository;

import org.example.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class UniversityRepository {
    private final JdbcTemplate jdbc;
    public UniversityRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> academicRecords(long applicantId) {
        return jdbc.queryForList("SELECT academic_record_id AS \"academicRecordId\", applicant_id AS \"applicantId\", " +
                "qualification AS \"qualification\", result AS \"result\", graduation_year AS \"graduationYear\", created_at AS \"createdAt\" " +
                "FROM academic_record WHERE applicant_id=? ORDER BY graduation_year DESC NULLS LAST, academic_record_id DESC", applicantId);
    }

    public long createAcademicRecord(long applicantId, String qualification, String result, Integer year) {
        jdbc.update("INSERT INTO academic_record (applicant_id, qualification, result, graduation_year) VALUES (?, ?, ?, ?)",
                applicantId, qualification, result, year);
        return jdbc.queryForObject("SELECT seq_academic.CURRVAL FROM dual", Long.class);
    }

    public void updateAcademicRecord(long recordId, long applicantId, String qualification, String result, Integer year) {
        int rows = jdbc.update("UPDATE academic_record SET qualification=?, result=?, graduation_year=? " +
                "WHERE academic_record_id=? AND applicant_id=?", qualification, result, year, recordId, applicantId);
        if (rows == 0) throw new ResourceNotFoundException("Academic record not found.");
    }

    public List<Map<String, Object>> universities(Long countryId) {
        String sql = "SELECT u.university_id AS \"universityId\", u.country_id AS \"countryId\", " +
                "c.country_name AS \"country\", u.university_name AS \"universityName\", u.website AS \"website\", " +
                "u.entry_criteria AS \"entryCriteria\" FROM university u JOIN country c ON c.country_id=u.country_id " +
                (countryId == null ? "" : "WHERE u.country_id=? ") + "ORDER BY c.country_name, u.university_name";
        return countryId == null ? jdbc.queryForList(sql) : jdbc.queryForList(sql, countryId);
    }

    public long createRecommendation(long applicantId, long academicRecordId, String note) {
        jdbc.update("INSERT INTO university_recommendation (applicant_id, academic_record_id, recommendation_note) VALUES (?, ?, ?)",
                applicantId, academicRecordId, note);
        return jdbc.queryForObject("SELECT seq_univ_recom.CURRVAL FROM dual", Long.class);
    }

    public void addRecommendedUniversity(long recommendationId, long universityId, Integer rank, String note) {
        jdbc.update("INSERT INTO recommendation_university (recommendation_id, university_id, rank_no, match_note) VALUES (?, ?, ?, ?)",
                recommendationId, universityId, rank, note);
    }

    public List<Map<String, Object>> recommendations(Long applicantId) {
        String sql = "SELECT r.recommendation_id AS \"recommendationId\", r.applicant_id AS \"applicantId\", " +
                "r.academic_record_id AS \"academicRecordId\", r.suggested_at AS \"suggestedAt\", " +
                "r.recommendation_note AS \"recommendationNote\", ar.qualification AS \"qualification\", ar.result AS \"result\", " +
                "ar.graduation_year AS \"graduationYear\", u.university_id AS \"universityId\", " +
                "u.university_name AS \"universityName\", c.country_name AS \"country\", ru.rank_no AS \"rankNo\", " +
                "ru.match_note AS \"matchNote\", u.website AS \"website\" " +
                "FROM university_recommendation r JOIN academic_record ar ON ar.academic_record_id=r.academic_record_id " +
                "AND ar.applicant_id=r.applicant_id LEFT JOIN recommendation_university ru ON ru.recommendation_id=r.recommendation_id " +
                "LEFT JOIN university u ON u.university_id=ru.university_id LEFT JOIN country c ON c.country_id=u.country_id " +
                (applicantId == null ? "" : "WHERE r.applicant_id=? ") + "ORDER BY r.suggested_at DESC, ru.rank_no NULLS LAST";
        return applicantId == null ? jdbc.queryForList(sql) : jdbc.queryForList(sql, applicantId);
    }

    public boolean academicRecordBelongs(long recordId, long applicantId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM academic_record WHERE academic_record_id=? AND applicant_id=?",
                Integer.class, recordId, applicantId);
        return count != null && count > 0;
    }
}
