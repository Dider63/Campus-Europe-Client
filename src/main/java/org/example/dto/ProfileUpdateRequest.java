package org.example.dto;

public final class ProfileUpdateRequest extends RequestPayload {
    public String fullName() { return stringField("fullName"); }
    public String email() { return stringField("email"); }
    public String phone() { return stringField("phone"); }
    public Long nationalityCountryId() { return longField("nationalityCountryId"); }
}
