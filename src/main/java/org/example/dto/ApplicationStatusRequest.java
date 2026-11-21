package org.example.dto;

public final class ApplicationStatusRequest extends RequestPayload {
    public String status() { return stringField("status"); }
    public String note() { return stringField("note"); }
}
