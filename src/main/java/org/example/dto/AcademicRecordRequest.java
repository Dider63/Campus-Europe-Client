package org.example.dto;

/** Request DTO for AcademicRecord operations; preserves the original JSON payload. */
public final class AcademicRecordRequest extends RequestPayload {
    public String qualification() { return stringField("qualification"); }
    public String result() { return stringField("result"); }
    public Long graduationYear() { return longField("graduationYear"); }
}
