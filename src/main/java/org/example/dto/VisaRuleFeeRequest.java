package org.example.dto;

public final class VisaRuleFeeRequest extends RequestPayload {
    public java.math.BigDecimal serviceFee() { return decimalField("serviceFee"); }
    public String currencyCode() { return stringField("currencyCode"); }
    public String changeNote() { return stringField("changeNote"); }
}
