package org.example.repository;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
public class AdminRepository {
    private final JdbcTemplate jdbc;
    public AdminRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long createCountry(String name, String code, String active) {
        jdbc.update("INSERT INTO country (country_name, destination_code, is_active) VALUES (?, ?, ?)", name, code, active);
        return jdbc.queryForObject("SELECT seq_country.CURRVAL FROM dual", Long.class);
    }
    public void updateCountry(long id, String name, String code, String active) {
        ensureUpdated(jdbc.update("UPDATE country SET country_name=?, destination_code=?, is_active=? WHERE country_id=?", name, code, active, id), "Country");
    }

    public long createVisaType(String name, String description, String active) {
        jdbc.update("INSERT INTO visa_type (type_name, description, is_active) VALUES (?, ?, ?)", name, description, active);
        return jdbc.queryForObject("SELECT seq_visa_type.CURRVAL FROM dual", Long.class);
    }
    public void updateVisaType(long id, String name, String description, String active) {
        ensureUpdated(jdbc.update("UPDATE visa_type SET type_name=?, description=?, is_active=? WHERE visa_type_id=?", name, description, active, id), "Visa type");
    }

    public long createVisaRule(long visaTypeId, long destinationId, long nationalityId, BigDecimal fee,
                               String currency, String processing, String active, long updatedBy, String note) {
        jdbc.update("INSERT INTO visa_rule (visa_type_id, destination_country_id, nationality_country_id, service_fee, " +
                "currency_code, processing_time, is_active, updated_by_user_id, update_note) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                visaTypeId, destinationId, nationalityId, fee, currency, processing, active, updatedBy, note);
        return jdbc.queryForObject("SELECT seq_visa_rule.CURRVAL FROM dual", Long.class);
    }

    public void updateVisaRule(long id, long visaTypeId, long destinationId, long nationalityId,
                               String processing, String active, long updatedBy, String note) {
        ensureUpdated(jdbc.update("UPDATE visa_rule SET visa_type_id=?, destination_country_id=?, nationality_country_id=?, " +
                "processing_time=?, is_active=?, updated_by_user_id=?, update_note=? WHERE visa_rule_id=?",
                visaTypeId, destinationId, nationalityId, processing, active, updatedBy, note, id), "Visa rule");
    }

    public List<Map<String, Object>> visaRules() {
        return jdbc.queryForList("SELECT vr.visa_rule_id AS \"visaRuleId\", vr.visa_type_id AS \"visaTypeId\", " +
                "vr.destination_country_id AS \"destinationCountryId\", vr.nationality_country_id AS \"nationalityCountryId\", vt.type_name AS \"visaTypeName\", " +
                "d.country_name AS \"destinationCountry\", n.country_name AS \"applicantNationality\", " +
                "vr.service_fee AS \"serviceFee\", vr.currency_code AS \"currencyCode\", " +
                "vr.processing_time AS \"processingTime\", vr.is_active AS \"isActive\", " +
                "vr.last_updated AS \"lastUpdated\", vr.update_note AS \"updateNote\" " +
                "FROM visa_rule vr JOIN visa_type vt ON vt.visa_type_id=vr.visa_type_id " +
                "JOIN country d ON d.country_id=vr.destination_country_id " +
                "JOIN country n ON n.country_id=vr.nationality_country_id ORDER BY d.country_name, vt.type_name");
    }

    public void updateVisaRuleActive(long id, String active, long userId, String note) {
        ensureUpdated(jdbc.update("UPDATE visa_rule SET is_active=?, updated_by_user_id=?, update_note=? WHERE visa_rule_id=?",
                active, userId, note, id), "Visa rule");
    }

    public void updateFee(long ruleId, BigDecimal fee, String currency, Long userId, String note) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (CallableStatement call = connection.prepareCall("{call update_visa_rule_fee(?, ?, ?, ?, ?)}")) {
                call.setLong(1, ruleId);
                call.setBigDecimal(2, fee);
                call.setString(3, currency);
                if (userId == null) call.setNull(4, Types.NUMERIC); else call.setLong(4, userId);
                call.setString(5, note);
                call.execute();
            }
            return null;
        });
    }

    public List<Map<String, Object>> feeHistory(long ruleId) {
        return jdbc.queryForList("SELECT fee_history_id AS \"feeHistoryId\", old_service_fee AS \"oldServiceFee\", " +
                "new_service_fee AS \"newServiceFee\", old_currency_code AS \"oldCurrencyCode\", " +
                "new_currency_code AS \"newCurrencyCode\", changed_at AS \"changedAt\", change_note AS \"changeNote\" " +
                "FROM visa_rule_fee_history WHERE visa_rule_id=? ORDER BY changed_at DESC", ruleId);
    }

    public List<Map<String, Object>> checklistItems() {
        return jdbc.queryForList("SELECT checklist_item_id AS \"checklistItemId\", item_name AS \"itemName\", " +
                "item_description AS \"itemDescription\", is_active AS \"isActive\" FROM checklist_item ORDER BY item_name");
    }
    public long createChecklistItem(String name, String description, String active) {
        jdbc.update("INSERT INTO checklist_item (item_name, item_description, is_active) VALUES (?, ?, ?)", name, description, active);
        return jdbc.queryForObject("SELECT seq_check_item.CURRVAL FROM dual", Long.class);
    }
    public void updateChecklistItem(long id, String name, String description, String active) {
        ensureUpdated(jdbc.update("UPDATE checklist_item SET item_name=?, item_description=?, is_active=? WHERE checklist_item_id=?",
                name, description, active, id), "Checklist item");
    }
    public void attachChecklist(long ruleId, long itemId, String required, int order, String instructions) {
        jdbc.update("MERGE INTO visa_rule_checklist v USING (SELECT ? visa_rule_id, ? checklist_item_id FROM dual) s " +
                "ON (v.visa_rule_id=s.visa_rule_id AND v.checklist_item_id=s.checklist_item_id) " +
                "WHEN MATCHED THEN UPDATE SET is_required=?, display_order=?, instructions=? " +
                "WHEN NOT MATCHED THEN INSERT (visa_rule_id, checklist_item_id, is_required, display_order, instructions) " +
                "VALUES (s.visa_rule_id, s.checklist_item_id, ?, ?, ?)",
                ruleId, itemId, required, order, instructions, required, order, instructions);
    }
    public void detachChecklist(long ruleId, long itemId) {
        jdbc.update("DELETE FROM visa_rule_checklist WHERE visa_rule_id=? AND checklist_item_id=?", ruleId, itemId);
    }

    public List<Map<String, Object>> universities() {
        return jdbc.queryForList("SELECT u.university_id AS \"universityId\", u.country_id AS \"countryId\", " +
                "c.country_name AS \"country\", u.university_name AS \"universityName\", u.website AS \"website\", " +
                "u.entry_criteria AS \"entryCriteria\" " +
                "FROM university u JOIN country c ON c.country_id=u.country_id ORDER BY c.country_name, u.university_name");
    }
    public long createUniversity(long countryId, String name, String website, String criteria) {
        jdbc.update("INSERT INTO university (country_id, university_name, website, entry_criteria) VALUES (?, ?, ?, ?)",
                countryId, name, website, criteria);
        return jdbc.queryForObject("SELECT seq_university.CURRVAL FROM dual", Long.class);
    }
    public void updateUniversity(long id, long countryId, String name, String website, String criteria) {
        ensureUpdated(jdbc.update("UPDATE university SET country_id=?, university_name=?, website=?, entry_criteria=? WHERE university_id=?",
                countryId, name, website, criteria, id), "University");
    }

    public Map<String, Object> dashboardReport() {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("totalApplicants", scalar("SELECT COUNT(*) FROM applicant"));
        m.put("totalApplications", scalar("SELECT COUNT(*) FROM visa_application"));
        m.put("draftApplications", scalar("SELECT COUNT(*) FROM visa_application WHERE current_status='DRAFT'"));
        m.put("submittedApplications", scalar("SELECT COUNT(*) FROM visa_application WHERE current_status='SUBMITTED'"));
        m.put("underReviewApplications", scalar("SELECT COUNT(*) FROM visa_application WHERE current_status='UNDER_REVIEW'"));
        m.put("processingApplications", scalar("SELECT COUNT(*) FROM visa_application WHERE current_status='PROCESSING'"));
        m.put("approvedApplications", scalar("SELECT COUNT(*) FROM visa_application WHERE current_status='APPROVED'"));
        m.put("rejectedApplications", scalar("SELECT COUNT(*) FROM visa_application WHERE current_status='REJECTED'"));
        m.put("pendingPayments", scalar("SELECT COUNT(*) FROM payment_record WHERE payment_status IN ('PENDING','PARTIAL')"));
        m.put("completedPayments", scalar("SELECT COUNT(*) FROM payment_record WHERE payment_status='PAID'"));
        m.put("consultationRequests", scalar("SELECT COUNT(*) FROM consultant_appointment WHERE appointment_status='REQUESTED'"));
        m.put("confirmedConsultations", scalar("SELECT COUNT(*) FROM consultant_appointment WHERE appointment_status='CONFIRMED'"));
        m.put("applicationsByDestination", jdbc.queryForList("SELECT c.country_name AS \"label\", COUNT(*) AS \"total\" " +
                "FROM visa_application a JOIN visa_rule vr ON vr.visa_rule_id=a.visa_rule_id " +
                "JOIN country c ON c.country_id=vr.destination_country_id GROUP BY c.country_name ORDER BY \"total\" DESC"));
        m.put("applicationsByVisaType", jdbc.queryForList("SELECT vt.type_name AS \"label\", COUNT(*) AS \"total\" " +
                "FROM visa_application a JOIN visa_rule vr ON vr.visa_rule_id=a.visa_rule_id " +
                "JOIN visa_type vt ON vt.visa_type_id=vr.visa_type_id GROUP BY vt.type_name ORDER BY \"total\" DESC"));
        m.put("recentApplications", jdbc.queryForList("SELECT application_id AS \"applicationId\", current_status AS \"currentStatus\", " +
                "application_date AS \"applicationDate\", applicant_name AS \"applicantName\", visa_type_name AS \"visaTypeName\", " +
                "destination_country AS \"destinationCountry\" FROM vw_application_tracking ORDER BY application_date DESC FETCH FIRST 10 ROWS ONLY"));
        return m;
    }

    private Long scalar(String sql) { return jdbc.queryForObject(sql, Long.class); }
    private void ensureUpdated(int rows, String resource) {
        if (rows == 0) throw new org.example.exception.ResourceNotFoundException(resource + " not found.");
    }
}
