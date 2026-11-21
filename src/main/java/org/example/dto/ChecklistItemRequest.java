package org.example.dto;

public final class ChecklistItemRequest extends RequestPayload {
    public String itemName() { return stringField("itemName"); }
    public String itemDescription() { return stringField("itemDescription"); }
    public Boolean isActive() { return booleanField("isActive"); }
}
