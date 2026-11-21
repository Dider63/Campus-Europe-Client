package org.example.dto;

public final class LoginRequest extends RequestPayload {
    public String username() { return stringField("username"); }
    public String password() { return stringField("password"); }
}
