package org.example.service;


import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.ZoneId;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowService {
    private final WorkflowRepository workflowRepository;
    private final ApplicationService applicationService;
    private final ApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final GoogleMeetService googleMeetService;
    private final ZoneId agencyZone;

    public WorkflowService(WorkflowRepository workflowRepository, ApplicationService applicationService,
                           ApplicationRepository applicationRepository, AccountRepository accountRepository,
                           GoogleMeetService googleMeetService,
                           @Value("${app.time-zone:Asia/Dhaka}") String agencyTimeZone) {
        this.workflowRepository = workflowRepository;
        this.applicationService = applicationService;
        this.applicationRepository = applicationRepository;
        this.accountRepository = accountRepository;
        this.googleMeetService = googleMeetService;
        this.agencyZone = ZoneId.of(agencyTimeZone);
    }

    public Map<String, Object> meetingIntegration() {
        ApplicationService.requireStaffRole(CurrentUser.get());
        return Map.of("configured", googleMeetService.isConfigured(),
                "provider", "Google Meet", "mode", "OAuth 2.0 / Meet REST API");
    }

    public List<Map<String, Object>> payments(long applicationId) {
        applicationService.ensureCanAccess(applicationId);
        return workflowRepository.payments(applicationId);
    }

    @Transactional
    public Map<String, Object> recordPayment(long applicationId, PaymentRequest body) {
        CustomUserDetails user = CurrentUser.get();
        ApplicationService.requireStaffRole(user);
        Long staffId = accountRepository.staffId(user.getUserId());
        if (staffId == null) throw new BusinessException("An active staff profile is required to record payments.");
        applicationRepository.getApplication(applicationId);
        BigDecimal amount = RequestValues.decimal(body, "amountPaid");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amountPaid must be greater than zero.");
        BigDecimal fee = workflowRepository.lockFeeDue(applicationId);
        BigDecimal paid = workflowRepository.totalPaid(applicationId);
        BigDecimal remaining = fee.subtract(paid);
        if (remaining.signum() <= 0) throw new BusinessException("The application fee has already been fully recorded as paid.");
        if (amount.compareTo(remaining) > 0) throw new IllegalArgumentException("Payment exceeds the remaining service fee of " + remaining + ".");
        String method = body.get("paymentMethod") == null ? null : String.valueOf(body.get("paymentMethod")).trim();
        String reference = body.get("transactionReference") == null ? null : String.valueOf(body.get("transactionReference")).trim();
        if (method != null && method.length() > 40) throw new IllegalArgumentException("Payment method must be at most 40 characters.");
        if (reference != null && reference.length() > 120) throw new IllegalArgumentException("Transaction reference must be at most 120 characters.");
        BigDecimal newTotal = paid.add(amount);
        String status = newTotal.compareTo(fee) >= 0 ? "PAID" : "PARTIAL";
        String invoice = "VPA-" + applicationId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        long paymentId = workflowRepository.insertPayment(applicationId, amount, status, invoice, method, reference, staffId);
        return Map.of("paymentId", paymentId, "invoiceNumber", invoice, "paymentStatus", status,
                "amountPaid", amount, "totalPaid", newTotal, "remainingAmount", fee.subtract(newTotal),
                "note", "This records an offline/manual payment; no payment gateway is connected.");
    }


    @Transactional
    public Map<String, Object> updatePayment(long paymentId, PaymentRequest body) {
        CustomUserDetails user = CurrentUser.get();
        ApplicationService.requireStaffRole(user);
        Long staffId = accountRepository.staffId(user.getUserId());
        if (staffId == null) throw new BusinessException("An active staff profile is required to update payments.");

        Map<String, Object> existing = workflowRepository.paymentMeta(paymentId);
        long applicationId = ((Number) existing.get("applicationId")).longValue();
        BigDecimal amount = RequestValues.decimal(body, "amountPaid");
        if (amount == null || amount.signum() < 0) throw new IllegalArgumentException("amountPaid must be zero or greater.");
        String status = RequestValues.required(body, "paymentStatus", 20).toUpperCase(Locale.ROOT);
        if (!List.of("PENDING", "PARTIAL", "PAID", "FAILED", "REFUNDED", "CANCELLED").contains(status))
            throw new IllegalArgumentException("Invalid payment status.");
        if (List.of("PARTIAL", "PAID").contains(status) && amount.signum() <= 0)
            throw new IllegalArgumentException("PARTIAL and PAID records must have an amount greater than zero.");
        BigDecimal fee = workflowRepository.lockFeeDue(applicationId);
        BigDecimal otherPaid = workflowRepository.totalPaidExcluding(applicationId, paymentId);
        if (List.of("PARTIAL", "PAID").contains(status) && otherPaid.add(amount).compareTo(fee) > 0)
            throw new IllegalArgumentException("The updated payment would exceed the application's service fee.");
        BigDecimal updatedRecognizedTotal = otherPaid.add(List.of("PARTIAL", "PAID").contains(status) ? amount : BigDecimal.ZERO);
        if ("PAID".equals(status) && updatedRecognizedTotal.compareTo(fee) != 0)
            throw new IllegalArgumentException("A PAID status requires recognized payments to equal the full service fee.");
        if ("PARTIAL".equals(status) && updatedRecognizedTotal.compareTo(fee) >= 0)
            throw new IllegalArgumentException("Use PAID when recognized payments reach the full service fee.");
        String method = body.get("paymentMethod") == null ? null : String.valueOf(body.get("paymentMethod")).trim();
        String reference = body.get("transactionReference") == null ? null : String.valueOf(body.get("transactionReference")).trim();
        if (method != null && method.length() > 40) throw new IllegalArgumentException("Payment method must be at most 40 characters.");
        if (reference != null && reference.length() > 120) throw new IllegalArgumentException("Transaction reference must be at most 120 characters.");
        workflowRepository.updatePayment(paymentId, amount, status, method, reference);
        return Map.of("paymentId", paymentId, "applicationId", applicationId, "paymentStatus", status,
                "amountPaid", amount, "totalRecognizedPaid", updatedRecognizedTotal,
                "remainingAmount", fee.subtract(updatedRecognizedTotal));
    }

    public Map<String, Object> invoice(long paymentId) {
        Map<String, Object> invoice = workflowRepository.invoice(paymentId);
        CustomUserDetails user = CurrentUser.get();
        if (user.hasRole("USER")) {
            Long applicantId = accountRepository.applicantId(user.getUserId());
            if (applicantId == null) throw new org.springframework.security.access.AccessDeniedException("Access denied.");
            applicationRepository.assertOwned(((Number) invoice.get("applicationId")).longValue(), applicantId);
        } else ApplicationService.requireStaffRole(user);
        return invoice;
    }

    public List<Map<String, Object>> appointments() {
        CustomUserDetails user = CurrentUser.get();
        if (user.hasRole("USER")) return workflowRepository.appointments(applicationService.applicantIdForCurrentUser());
        ApplicationService.requireStaffRole(user);
        return workflowRepository.appointments(null);
    }

    @Transactional
    public Map<String, Object> requestAppointment(AppointmentRequest body) {
        CustomUserDetails user = CurrentUser.get();
        if (!user.hasRole("USER")) throw new org.springframework.security.access.AccessDeniedException("Only applicants can request appointments.");
        LocalDateTime requested = RequestValues.dateTime(body, "requestedDatetime");
        if (requested == null || requested.isBefore(LocalDateTime.now(agencyZone).plusMinutes(5)))
            throw new IllegalArgumentException("Choose an appointment time at least five minutes in the future.");
        Long durationValue = RequestValues.longValue(body, "durationMinutes");
        int duration = durationValue == null ? 30 : Math.toIntExact(durationValue);
        if (duration < 1 || duration > 600) throw new IllegalArgumentException("Duration must be between 1 and 600 minutes.");
        long id = workflowRepository.createAppointment(applicationService.applicantIdForCurrentUser(), requested, duration);
        return Map.of("appointmentId", id, "appointmentStatus", "REQUESTED");
    }

    @Transactional
    public void confirmAppointment(long appointmentId, AppointmentConfirmationRequest body) {
        CustomUserDetails user = CurrentUser.get();
        ApplicationService.requireStaffRole(user);
        Long staffId = accountRepository.staffId(user.getUserId());
        if (staffId == null) throw new BusinessException("An active staff profile is required to confirm appointments.");
        Map<String, Object> appointment = workflowRepository.appointment(appointmentId);
        if (!"REQUESTED".equals(appointment.get("appointmentStatus")))
            throw new BusinessException("Only requested appointments may be confirmed.");
        String link = body.get("meetingLink") == null ? null : String.valueOf(body.get("meetingLink")).trim();
        if (link != null && !link.isBlank()) {
            if (link.length() > 1000) throw new IllegalArgumentException("Meeting link must be at most 1000 characters.");
            try {
                URI uri = new URI(link);
                if (!"https".equalsIgnoreCase(uri.getScheme()) || !"meet.google.com".equalsIgnoreCase(uri.getHost())
                        || uri.getUserInfo() != null || uri.getPath() == null || uri.getPath().length() < 2)
                    throw new IllegalArgumentException("Provide a secure Google Meet link (https://meet.google.com/...).");
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException("Meeting link is not a valid URL.");
            }
        }
        LocalDateTime confirmed = RequestValues.dateTime(body, "confirmedDatetime");
        if (confirmed != null && confirmed.isBefore(LocalDateTime.now(agencyZone)))
            throw new IllegalArgumentException("Confirmed appointment time cannot be in the past.");
        if (confirmed == null && appointment.get("requestedDatetime") instanceof java.sql.Timestamp timestamp
                && timestamp.toLocalDateTime().isBefore(LocalDateTime.now(agencyZone)))
            throw new IllegalArgumentException("The requested appointment time has passed; reschedule instead.");
        // Create a real Meet space only when staff has not provided a verified link.
        // No fake URL or silent fallback is ever stored.
        if (link == null || link.isBlank()) link = googleMeetService.createMeeting();
        workflowRepository.confirmAppointment(appointmentId, staffId, confirmed, link);
    }

    @Transactional
    public void completeAppointment(long appointmentId) {
        CustomUserDetails user = CurrentUser.get();
        ApplicationService.requireStaffRole(user);
        if (accountRepository.staffId(user.getUserId()) == null)
            throw new BusinessException("An active staff profile is required to complete appointments.");
        workflowRepository.completeAppointment(appointmentId);
    }

    @Transactional
    public void cancelAppointment(long appointmentId) {
        CustomUserDetails user = CurrentUser.get();
        if (user.hasRole("USER")) {
            workflowRepository.cancelAppointment(appointmentId, applicationService.applicantIdForCurrentUser());
        } else {
            ApplicationService.requireStaffRole(user);
            if (accountRepository.staffId(user.getUserId()) == null) throw new BusinessException("An active staff profile is required.");
            workflowRepository.cancelAppointment(appointmentId, null);
        }
    }
}

