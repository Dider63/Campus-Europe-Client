package org.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class RequestPayload extends LinkedHashMap<String, Object> {
    protected String stringField(String name) { return RequestValues.text(this, name); }
    protected Long longField(String name) { return RequestValues.longValue(this, name); }
    protected BigDecimal decimalField(String name) { return RequestValues.decimal(this, name); }
    protected Boolean booleanField(String name) {
        if (!containsKey(name) || get(name) == null) return null;
        return RequestValues.bool(this, name, false);
    }
    protected LocalDateTime dateTimeField(String name) { return RequestValues.dateTime(this, name); }
    protected List<Object> listField(String name) { return RequestValues.list(this, name); }
}
