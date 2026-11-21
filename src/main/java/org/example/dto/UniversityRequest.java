package org.example.dto;

public final class UniversityRequest extends RequestPayload {
    public Long countryId() { return longField("countryId"); }
    public String universityName() { return stringField("universityName"); }
    public String website() { return stringField("website"); }
    public String entryCriteria() { return stringField("entryCriteria"); }
}
