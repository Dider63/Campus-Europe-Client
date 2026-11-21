package org.example.dto;

public final class StaffRequest extends RequestPayload {
    public String fullName() { return stringField("fullName"); }
    public String username() { return stringField("username"); }
    public String email() { return stringField("email"); }
    public String password() { return stringField("password"); }
    public String role() { return stringField("role"); }
    public String staffTitle() { return stringField("staffTitle"); }
    public String accountStatus() { return stringField("accountStatus"); }
    public Boolean isActive() { return booleanField("isActive"); }
}
