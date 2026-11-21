package org.example.repository;

import org.example.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class WorkflowRepository {
    private final JdbcTemplate jdbc;
    public WorkflowRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long insertDocument(long applicationId, Long checklistItemId, String fileName,
                               String fileType, String filePath, long uploadedBy) {
        jdbc.update("INSERT INTO application_document (application_id, checklist_item_id, file_name, file_type, file_path, uploaded_by_user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)", applicationId, checklistItemId, fileName, fileType, filePath, uploadedBy);
        return jdbc.queryForObject("SELECT seq_app_doc.CURRVAL FROM dual", Long.class);
    }

    public List<Map<String, Object>> documents(long applicationId) {
        return jdbc.queryForList("SELECT d.document_id AS \"documentId\", d.checklist_item_id AS \"checklistItemId\", " +
                "ci.item_name AS \"itemName\", d.file_name AS \"fileName\", d.file_type AS \"fileType\", " +
                "d.upload_date AS \"uploadDate\", d.review_status AS \"reviewStatus\", d.reviewed_at AS \"reviewedAt\", " +
                "d.review_note AS \"reviewNote\", reviewer.full_name AS \"reviewedBy\" " +
                "FROM application_document d LEFT JOIN checklist_item ci ON ci.checklist_item_id = d.checklist_item_id " +
                "LEFT JOIN staff_profile s ON s.staff_id = d.reviewed_by_staff_id " +
                "LEFT JOIN user_account reviewer ON reviewer.user_id = s.user_id " +
                "WHERE d.application_id = ? ORDER BY d.upload_date DESC, d.document_id DESC", applicationId);
    }

    public boolean latestDocumentNeedsCorrection(long applicationId, Long checklistItemId) {
        String sql = "SELECT review_status FROM application_document WHERE application_id = ? AND " +
                (checklistItemId == null ? "checklist_item_id IS NULL " : "checklist_item_id = ? ") +
                "ORDER BY upload_date DESC, document_id DESC FETCH FIRST 1 ROWS ONLY";
        List<String> statuses = checklistItemId == null
                ? jdbc.query(sql, (rs, rowNum) -> rs.getString(1), applicationId)
                : jdbc.query(sql, (rs, rowNum) -> rs.getString(1), applicationId, checklistItemId);
        return !statuses.isEmpty() && ("REJECTED".equals(statuses.getFirst()) || "NEEDS_CORRECTION".equals(statuses.getFirst()));
    }

    public Map<String, Object> documentStorage(long documentId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT document_id AS \"documentId\", application_id AS \"applicationId\", " +
                "file_name AS \"fileName\", file_type AS \"fileType\", file_path AS \"filePath\" " +
                "FROM application_document WHERE document_id = ?", documentId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Document not found.");
        return rows.getFirst();
    }

    public void reviewDocument(long documentId, String status, long staffId, String note) {
        int changed = jdbc.update("UPDATE application_document SET review_status = ?, reviewed_by_staff_id = ?, review_note = ? " +
                "WHERE document_id = ?", status, staffId, note, documentId);
        if (changed == 0) throw new ResourceNotFoundException("Document not found.");
    }

    public long insertPayment(long applicationId, BigDecimal amountPaid, String paymentStatus,
                              String invoiceNumber, String paymentMethod, String transactionReference, long staffId) {
        jdbc.update("INSERT INTO payment_record (application_id, amount_paid, payment_date, payment_status, invoice_number, " +
                "payment_method, transaction_reference, recorded_by_staff_id) " +
                "VALUES (?, ?, SYSTIMESTAMP, ?, ?, ?, ?, ?)", applicationId, amountPaid, paymentStatus,
                invoiceNumber, paymentMethod, transactionReference, staffId);
        return jdbc.queryForObject("SELECT seq_payment.CURRVAL FROM dual", Long.class);
    }


    public Map<String, Object> paymentMeta(long paymentId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT payment_id AS \"paymentId\", application_id AS \"applicationId\", " +
                "amount_paid AS \"amountPaid\", payment_status AS \"paymentStatus\" FROM payment_record WHERE payment_id = ?", paymentId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Payment record not found.");
        return rows.getFirst();
    }

    public BigDecimal totalPaidExcluding(long applicationId, long paymentId) {
        BigDecimal value = jdbc.queryForObject("SELECT NVL(SUM(amount_paid), 0) FROM payment_record " +
                "WHERE application_id = ? AND payment_id <> ? AND payment_status IN ('PARTIAL', 'PAID')",
                BigDecimal.class, applicationId, paymentId);
        return value == null ? BigDecimal.ZERO : value;
    }

    public void updatePayment(long paymentId, BigDecimal amountPaid, String paymentStatus,
                              String paymentMethod, String transactionReference) {
        int changed = jdbc.update("UPDATE payment_record SET amount_paid = ?, payment_status = ?, " +
                "payment_date = CASE WHEN ? IN ('PARTIAL', 'PAID') THEN NVL(payment_date, SYSTIMESTAMP) ELSE payment_date END, " +
                "payment_method = ?, transaction_reference = ? WHERE payment_id = ?",
                amountPaid, paymentStatus, paymentStatus, paymentMethod, transactionReference, paymentId);
        if (changed == 0) throw new ResourceNotFoundException("Payment record not found.");
    }

    public List<Map<String, Object>> payments(long applicationId) {
        return jdbc.queryForList("SELECT payment_id AS \"paymentId\", application_id AS \"applicationId\", " +
                "service_fee AS \"serviceFee\", currency_code AS \"currencyCode\", amount_paid AS \"amountPaid\", " +
                "payment_date AS \"paymentDate\", payment_status AS \"paymentStatus\", invoice_number AS \"invoiceNumber\", " +
                "payment_method AS \"paymentMethod\", transaction_reference AS \"transactionReference\" " +
                "FROM payment_record WHERE application_id = ? ORDER BY payment_date DESC NULLS LAST, payment_id DESC", applicationId);
    }

    public Map<String, Object> invoice(long paymentId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT p.payment_id AS \"paymentId\", p.invoice_number AS \"invoiceNumber\", " +
                "p.payment_date AS \"paymentDate\", p.service_fee AS \"serviceFee\", p.amount_paid AS \"amountPaid\", " +
                "p.currency_code AS \"currencyCode\", p.payment_status AS \"paymentStatus\", p.payment_method AS \"paymentMethod\", " +
                "a.application_id AS \"applicationId\", ua.full_name AS \"applicantName\", ua.email AS \"applicantEmail\", " +
                "vt.type_name AS \"visaTypeName\", dest.country_name AS \"destinationCountry\" " +
                "FROM payment_record p JOIN visa_application a ON a.application_id = p.application_id " +
                "JOIN applicant ap ON ap.applicant_id = a.applicant_id JOIN user_account ua ON ua.user_id = ap.user_id " +
                "JOIN visa_rule vr ON vr.visa_rule_id = a.visa_rule_id JOIN visa_type vt ON vt.visa_type_id = vr.visa_type_id " +
                "JOIN country dest ON dest.country_id = vr.destination_country_id WHERE p.payment_id = ?", paymentId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Invoice not found.");
        return rows.getFirst();
    }

    public BigDecimal totalPaid(long applicationId) {
        BigDecimal value = jdbc.queryForObject("SELECT NVL(SUM(amount_paid), 0) FROM payment_record WHERE application_id = ? " +
                "AND payment_status IN ('PARTIAL', 'PAID')", BigDecimal.class, applicationId);
        return value == null ? BigDecimal.ZERO : value;
    }

    public BigDecimal lockFeeDue(long applicationId) {
        return jdbc.queryForObject("SELECT service_fee_snapshot FROM visa_application WHERE application_id = ? FOR UPDATE",
                BigDecimal.class, applicationId);
    }

    public long createAppointment(long applicantId, LocalDateTime requested, int duration) {
        if (requested == null) {
            jdbc.update("INSERT INTO consultant_appointment (applicant_id, duration_minutes) VALUES (?, ?)", applicantId, duration);
        } else {
            jdbc.update("INSERT INTO consultant_appointment (applicant_id, requested_datetime, duration_minutes) VALUES (?, ?, ?)",
                    applicantId, Timestamp.valueOf(requested), duration);
        }
        return jdbc.queryForObject("SELECT seq_consult_appt.CURRVAL FROM dual", Long.class);
    }

    public List<Map<String, Object>> appointments(Long applicantId) {
        String sql = "SELECT a.appointment_id AS \"appointmentId\", a.applicant_id AS \"applicantId\", " +
                "u.full_name AS \"applicantName\", a.requested_datetime AS \"requestedDatetime\", " +
                "a.confirmed_datetime AS \"confirmedDatetime\", a.duration_minutes AS \"durationMinutes\", " +
                "a.meeting_link AS \"meetingLink\", a.appointment_status AS \"appointmentStatus\", " +
                "s.staff_id AS \"confirmedByStaffId\" FROM consultant_appointment a " +
                "JOIN applicant ap ON ap.applicant_id = a.applicant_id JOIN user_account u ON u.user_id = ap.user_id " +
                "LEFT JOIN staff_profile s ON s.staff_id = a.confirmed_by_staff_id " +
                (applicantId == null ? "" : "WHERE a.applicant_id = ? ") + "ORDER BY a.requested_datetime DESC";
        return applicantId == null ? jdbc.queryForList(sql) : jdbc.queryForList(sql, applicantId);
    }

    public Map<String, Object> appointment(long appointmentId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT appointment_id AS \"appointmentId\", applicant_id AS \"applicantId\", " +
                "appointment_status AS \"appointmentStatus\", requested_datetime AS \"requestedDatetime\", " +
                "meeting_link AS \"meetingLink\" FROM consultant_appointment WHERE appointment_id = ?", appointmentId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Appointment not found.");
        return rows.getFirst();
    }

    public void confirmAppointment(long appointmentId, long staffId, LocalDateTime confirmedAt, String meetingLink) {
        int changed = jdbc.update("UPDATE consultant_appointment SET appointment_status = 'CONFIRMED', " +
                "confirmed_datetime = NVL(?, requested_datetime), confirmed_by_staff_id = ?, meeting_link = ? " +
                "WHERE appointment_id = ? AND appointment_status = 'REQUESTED'",
                confirmedAt == null ? Timestamp.valueOf(LocalDateTime.now()) : Timestamp.valueOf(confirmedAt),
                staffId, meetingLink, appointmentId);
        if (changed == 0) throw new IllegalArgumentException("Only a requested appointment can be confirmed.");
    }

    public void completeAppointment(long appointmentId) {
        int changed = jdbc.update("UPDATE consultant_appointment SET appointment_status = 'COMPLETED' " +
                "WHERE appointment_id = ? AND appointment_status = 'CONFIRMED'", appointmentId);
        if (changed == 0) throw new IllegalArgumentException("Only a confirmed appointment can be marked completed.");
    }

    public void cancelAppointment(long appointmentId, Long applicantId) {
        String allowed = applicantId == null ? "('REQUESTED', 'CONFIRMED')" : "('REQUESTED')";
        String sql = "UPDATE consultant_appointment SET appointment_status = 'CANCELLED' WHERE appointment_id = ? " +
                "AND appointment_status IN " + allowed + (applicantId == null ? "" : " AND applicant_id = ?");
        int changed = applicantId == null ? jdbc.update(sql, appointmentId) : jdbc.update(sql, appointmentId, applicantId);
        if (changed == 0) throw new IllegalArgumentException("The appointment cannot be cancelled or was not found.");
    }
}
