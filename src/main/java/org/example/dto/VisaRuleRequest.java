package org.example.dto;

public final class VisaRuleRequest extends RequestPayload {
    public Long visaTypeId() { return longField("visaTypeId"); }
    public Long destinationCountryId() { return longField("destinationCountryId"); }
    public Long nationalityCountryId() { return longField("nationalityCountryId"); }
    public java.math.BigDecimal serviceFee() { return decimalField("serviceFee"); }
    public String currencyCode() { return stringField("currencyCode"); }
    public String processingTime() { return stringField("processingTime"); }
    public Boolean isActive() { return booleanField("isActive"); }
    public String updateNote() { return stringField("updateNote"); }
}
