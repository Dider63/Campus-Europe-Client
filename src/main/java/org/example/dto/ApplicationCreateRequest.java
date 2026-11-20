package org.example.dto;

/** Request DTO for ApplicationCreate operations; preserves the original JSON payload. */
public final class ApplicationCreateRequest extends RequestPayload {
    public Long visaRuleId() { return longField("visaRuleId"); }
}
