package org.example.repository;

import org.example.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class ApplicationRepository {
    private final JdbcTemplate jdbc;
    public ApplicationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long createApplication(long applicantId, long visaRuleId) {
        Long id = jdbc.execute((ConnectionCallback<Long>) connection -> {
            try (CallableStatement call = connection.prepareCall("{call create_visa_application(?, ?, ?)}")) {
                call.setLong(1, applicantId);
                call.setLong(2, visaRuleId);
                call.registerOutParameter(3, Types.NUMERIC);
                call.execute();
                return call.getLong(3);
            }
        });
        if (id == null || id <= 0) throw new IllegalStateException("Oracle did not return an application ID.");
        return id;
    }

    public List<Map<String, Object>> listForApplicant(long applicantId) {
        return jdbc.queryForList("SELECT t.application_id AS \"applicationId\", t.application_date AS \"applicationDate\", " +
                "t.submission_date AS \"submissionDate\", t.current_status AS \"currentStatus\", " +
                "t.visa_type_name AS \"visaTypeName\", t.destination_country AS \"destinationCountry\", " +
                "t.applicant_nationality AS \"applicantNationality\", t.service_fee AS \"serviceFee\", " +
                "t.currency_code AS \"currencyCode\", t.processing_time AS \"processingTime\", " +
                "a.last_updated AS \"lastUpdated\", a.last_status_note AS \"lastStatusNote\" " +
                "FROM vw_application_tracking t JOIN visa_application a ON a.application_id = t.application_id " +
                "WHERE t.applicant_id = ? ORDER BY t.application_date DESC", applicantId);
    }

    public List<Map<String, Object>> listAll(String status, String search) {
        StringBuilder sql = new StringBuilder("SELECT t.application_id AS \"applicationId\", t.application_date AS \"applicationDate\", " +
                "t.submission_date AS \"submissionDate\", t.current_status AS \"currentStatus\", " +
                "t.applicant_id AS \"applicantId\", t.applicant_name AS \"applicantName\", t.applicant_email AS \"applicantEmail\", " +
                "t.visa_type_name AS \"visaTypeName\", t.destination_country AS \"destinationCountry\", " +
                "t.applicant_nationality AS \"applicantNationality\", t.service_fee AS \"serviceFee\", " +
                "t.currency_code AS \"currencyCode\", a.last_updated AS \"lastUpdated\", a.last_status_note AS \"lastStatusNote\" " +
                "FROM vw_application_tracking t JOIN visa_application a ON a.application_id = t.application_id WHERE 1=1");
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (status != null && !status.isBlank()) { sql.append(" AND t.current_status = ?"); args.add(status); }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(t.applicant_name) LIKE ? OR LOWER(t.applicant_email) LIKE ? OR TO_CHAR(t.application_id) LIKE ?)");
            String q = "%" + search.toLowerCase() + "%";
            args.add(q); args.add(q); args.add(q);
        }
        sql.append(" ORDER BY t.application_date DESC");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> getApplication(long applicationId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT t.application_id AS \"applicationId\", " +
                "t.application_date AS \"applicationDate\", t.submission_date AS \"submissionDate\", " +
                "t.current_status AS \"currentStatus\", t.applicant_id AS \"applicantId\", " +
                "t.applicant_name AS \"applicantName\", t.applicant_email AS \"applicantEmail\", " +
                "t.visa_type_name AS \"visaTypeName\", t.destination_country AS \"destinationCountry\", " +
                "t.applicant_nationality AS \"applicantNationality\", t.service_fee AS \"serviceFee\", " +
                "t.currency_code AS \"currencyCode\", t.processing_time AS \"processingTime\", " +
                "a.visa_rule_id AS \"visaRuleId\", a.last_updated AS \"lastUpdated\", " +
                "a.last_updated_by_staff_id AS \"lastUpdatedByStaffId\", a.last_status_note AS \"lastStatusNote\" " +
                "FROM vw_application_tracking t JOIN visa_application a ON a.application_id = t.application_id " +
                "WHERE t.application_id = ?", applicationId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Application not found.");
        Map<String, Object> result = rows.getFirst();
        List<Map<String, Object>> details = jdbc.queryForList("SELECT passport_number AS \"passportNumber\", " +
                "passport_issue_date AS \"passportIssueDate\", passport_expiry_date AS \"passportExpiryDate\", " +
                "date_of_birth AS \"dateOfBirth\", gender AS \"gender\", marital_status AS \"maritalStatus\", occupation AS \"occupation\", " +
                "residential_address AS \"residentialAddress\", travel_purpose AS \"travelPurpose\", " +
                "expected_arrival_date AS \"expectedArrivalDate\", DBMS_LOB.SUBSTR(additional_info, 3900, 1) AS \"additionalInfo\" " +
                "FROM application_details WHERE application_id = ?", applicationId);
        result.put("details", details.isEmpty() ? Map.of() : details.getFirst());
        return result;
    }

    public void assertOwned(long applicationId, long applicantId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM visa_application WHERE application_id = ? AND applicant_id = ?", Integer.class,
                applicationId, applicantId);
        if (count == null || count == 0) throw new ResourceNotFoundException("Application not found.");
    }

    public String status(long applicationId) {
        List<String> rows = jdbc.query("SELECT current_status FROM visa_application WHERE application_id = ?",
                (rs, rowNum) -> rs.getString(1), applicationId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Application not found.");
        return rows.getFirst();
    }

    public void saveDetails(long applicationId, Map<String, Object> fields) {
        jdbc.update("MERGE INTO application_details d USING (SELECT ? application_id FROM dual) s " +
                "ON (d.application_id = s.application_id) " +
                "WHEN MATCHED THEN UPDATE SET passport_number=?, passport_issue_date=?, passport_expiry_date=?, date_of_birth=?, " +
                "gender=?, marital_status=?, occupation=?, residential_address=?, travel_purpose=?, expected_arrival_date=?, additional_info=? " +
                "WHEN NOT MATCHED THEN INSERT (application_id, passport_number, passport_issue_date, passport_expiry_date, date_of_birth, " +
                "gender, marital_status, occupation, residential_address, travel_purpose, expected_arrival_date, additional_info) " +
                "VALUES (s.application_id, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                applicationId,
                fields.get("passportNumber"), sqlDate(fields.get("passportIssueDate")), sqlDate(fields.get("passportExpiryDate")), sqlDate(fields.get("dateOfBirth")),
                fields.get("gender"), fields.get("maritalStatus"), fields.get("occupation"), fields.get("residentialAddress"),
                fields.get("travelPurpose"), sqlDate(fields.get("expectedArrivalDate")), fields.get("additionalInfo"),
                fields.get("passportNumber"), sqlDate(fields.get("passportIssueDate")), sqlDate(fields.get("passportExpiryDate")), sqlDate(fields.get("dateOfBirth")),
                fields.get("gender"), fields.get("maritalStatus"), fields.get("occupation"), fields.get("residentialAddress"),
                fields.get("travelPurpose"), sqlDate(fields.get("expectedArrivalDate")), fields.get("additionalInfo"));
    }

    private java.sql.Date sqlDate(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return java.sql.Date.valueOf(LocalDate.parse(String.valueOf(value)));
    }

    public List<Map<String, Object>> history(long applicationId) {
        return jdbc.queryForList("SELECT h.status_id AS \"statusId\", h.status AS \"status\", h.updated_at AS \"updatedAt\", " +
                "h.status_note AS \"statusNote\", u.full_name AS \"updatedBy\" " +
                "FROM application_status_history h LEFT JOIN staff_profile s ON s.staff_id = h.updated_by_staff_id " +
                "LEFT JOIN user_account u ON u.user_id = s.user_id WHERE h.application_id = ? " +
                "ORDER BY h.updated_at, h.status_id", applicationId);
    }

    public Map<String, Object> submissionDetails(long applicationId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT passport_number AS \"passportNumber\", " +
                "passport_issue_date AS \"passportIssueDate\", passport_expiry_date AS \"passportExpiryDate\", " +
                "date_of_birth AS \"dateOfBirth\", residential_address AS \"residentialAddress\", " +
                "travel_purpose AS \"travelPurpose\" FROM application_details WHERE application_id = ?", applicationId);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    public void submit(long applicationId) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (CallableStatement call = connection.prepareCall("{call submit_visa_application(?)}")) {
                call.setLong(1, applicationId);
                call.execute();
            }
            return null;
        });
    }

    public void changeStatus(long applicationId, String status, Long staffId, String note) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (CallableStatement call = connection.prepareCall("{call set_visa_app_status(?, ?, ?, ?)}")) {
                call.setLong(1, applicationId);
                call.setString(2, status);
                if (staffId == null) call.setNull(3, Types.NUMERIC); else call.setLong(3, staffId);
                call.setString(4, note);
                call.execute();
            }
            return null;
        });
    }
}
