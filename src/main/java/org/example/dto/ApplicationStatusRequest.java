package org.example.dto;

/** Request DTO for ApplicationStatus operations; preserves the original JSON payload. */
public final class ApplicationStatusRequest extends RequestPayload {
    public String status() { return stringField("status"); }
    public String note() { return stringField("note"); }
}
