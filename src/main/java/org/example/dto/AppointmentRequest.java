package com.nahidglobal.dto;

/** Request DTO for Appointment operations; preserves the original JSON payload. */
public final class AppointmentRequest extends RequestPayload {
    public java.time.LocalDateTime requestedDatetime() { return dateTimeField("requestedDatetime"); }
    public Long durationMinutes() { return longField("durationMinutes"); }
}
