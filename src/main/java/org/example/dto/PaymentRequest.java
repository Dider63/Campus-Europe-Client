package org.example.dto;

public final class PaymentRequest extends RequestPayload {
    public java.math.BigDecimal amountPaid() { return decimalField("amountPaid"); }
    public String paymentMethod() { return stringField("paymentMethod"); }
    public String transactionReference() { return stringField("transactionReference"); }
    public String paymentStatus() { return stringField("paymentStatus"); }
}
