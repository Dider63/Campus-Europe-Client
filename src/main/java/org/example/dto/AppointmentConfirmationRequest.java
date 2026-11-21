package org.example.dto;

public final class AppointmentConfirmationRequest extends RequestPayload {
    public java.time.LocalDateTime confirmedDatetime() { return dateTimeField("confirmedDatetime"); }
    public String meetingLink() { return stringField("meetingLink"); }
}
