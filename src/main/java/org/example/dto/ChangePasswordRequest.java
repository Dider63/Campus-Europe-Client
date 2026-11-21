package org.example.dto;

public final class ChangePasswordRequest extends RequestPayload {
    public String currentPassword() { return stringField("currentPassword"); }
    public String newPassword() { return stringField("newPassword"); }
    public String confirmPassword() { return stringField("confirmPassword"); }
}
