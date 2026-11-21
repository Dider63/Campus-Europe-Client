package org.example.dto;

public final class CountryRequest extends RequestPayload {
    public String countryName() { return stringField("countryName"); }
    public String destinationCode() { return stringField("destinationCode"); }
    public Boolean isActive() { return booleanField("isActive"); }
}
