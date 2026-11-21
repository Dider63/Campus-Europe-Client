package org.example.dto;

public final class ApplicationDetailsRequest extends RequestPayload {
    public String passportNumber() { return stringField("passportNumber"); }
    public String passportIssueDate() { return stringField("passportIssueDate"); }
    public String passportExpiryDate() { return stringField("passportExpiryDate"); }
    public String dateOfBirth() { return stringField("dateOfBirth"); }
    public String gender() { return stringField("gender"); }
    public String maritalStatus() { return stringField("maritalStatus"); }
    public String occupation() { return stringField("occupation"); }
    public String residentialAddress() { return stringField("residentialAddress"); }
    public String travelPurpose() { return stringField("travelPurpose"); }
    public String expectedArrivalDate() { return stringField("expectedArrivalDate"); }
    public String additionalInfo() { return stringField("additionalInfo"); }
}
