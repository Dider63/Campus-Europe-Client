package org.example.dto;

public final class UniversityRecommendationRequest extends RequestPayload {
    public Long applicantId() { return longField("applicantId"); }
    public Long academicRecordId() { return longField("academicRecordId"); }
    public String recommendationNote() { return stringField("recommendationNote"); }
    public java.util.List<Object> universities() { return listField("universities"); }
}
