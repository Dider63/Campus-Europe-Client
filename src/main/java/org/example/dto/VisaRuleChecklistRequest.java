package org.example.dto;

public final class VisaRuleChecklistRequest extends RequestPayload {
    public Long checklistItemId() { return longField("checklistItemId"); }
    public Boolean required() { return booleanField("required"); }
    public Long displayOrder() { return longField("displayOrder"); }
    public String instructions() { return stringField("instructions"); }
}
