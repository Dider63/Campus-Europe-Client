package org.example.dto;

public final class DocumentReviewRequest extends RequestPayload {
    public String reviewStatus() { return stringField("reviewStatus"); }
    public String reviewNote() { return stringField("reviewNote"); }
}
