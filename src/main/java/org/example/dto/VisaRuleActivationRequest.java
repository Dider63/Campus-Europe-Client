package org.example.dto;

public final class VisaRuleActivationRequest extends RequestPayload {
    public Boolean isActive() { return booleanField("isActive"); }
    public String updateNote() { return stringField("updateNote"); }
}
