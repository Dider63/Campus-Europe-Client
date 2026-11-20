package org.example.dto;

public final class ApplicationCreateRequest extends RequestPayload {
    public Long visaRuleId() { return longField("visaRuleId"); }
}
