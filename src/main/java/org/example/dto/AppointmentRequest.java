package org.example.dto;


public final class AppointmentRequest extends RequestPayload {
    public java.time.LocalDateTime requestedDatetime() { return dateTimeField("requestedDatetime"); }
    public Long durationMinutes() { return longField("durationMinutes"); }
}
