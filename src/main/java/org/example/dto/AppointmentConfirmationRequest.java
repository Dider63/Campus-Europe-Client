package org.example.dto;

/** Request DTO for AppointmentConfirmation operations; preserves the original JSON payload. */
public final class AppointmentConfirmationRequest extends RequestPayload {
    public java.time.LocalDateTime confirmedDatetime() { return dateTimeField("confirmedDatetime"); }
    public String meetingLink() { return stringField("meetingLink"); }
}
