package org.example.dto;

public final class RegisterRequest extends RequestPayload {
    public String fullName() { return stringField("fullName"); }
    public String username() { return stringField("username"); }
    public String email() { return stringField("email"); }
    public String password() { return stringField("password"); }
    public Long nationalityCountryId() { return longField("nationalityCountryId"); }
    public String phone() { return stringField("phone"); }
}
