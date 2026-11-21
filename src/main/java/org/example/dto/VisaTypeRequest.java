package org.example.dto;

public final class VisaTypeRequest extends RequestPayload {
    public String typeName() { return stringField("typeName"); }
    public String description() { return stringField("description"); }
    public Boolean isActive() { return booleanField("isActive"); }
}
