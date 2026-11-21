package org.example.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public final class RequestValues {
    private RequestValues() {}

    public static String text(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    public static String required(Map<String, Object> body, String key, int max) {
        String value = text(body, key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required.");
        if (value.length() > max) throw new IllegalArgumentException(key + " must be at most " + max + " characters.");
        return value;
    }

    public static Long longValue(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) return null;
        try { return Long.valueOf(String.valueOf(value)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(key + " must be a whole number."); }
    }

    public static long requiredLong(Map<String, Object> body, String key) {
        Long value = longValue(body, key);
        if (value == null || value <= 0) throw new IllegalArgumentException(key + " must be a positive number.");
        return value;
    }

    public static BigDecimal decimal(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) return null;
        try { return new BigDecimal(String.valueOf(value)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(key + " must be a valid number."); }
    }

    public static LocalDate date(Map<String, Object> body, String key) {
        String value = text(body, key);
        if (value == null || value.isBlank()) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException(key + " must use YYYY-MM-DD."); }
    }

    public static LocalDateTime dateTime(Map<String, Object> body, String key) {
        String value = text(body, key);
        if (value == null || value.isBlank()) return null;
        try { return LocalDateTime.parse(value); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException(key + " must be an ISO local date-time."); }
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) return List.of();
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException(key + " must be an array.");
        return (List<Object>) list;
    }

    public static boolean bool(Map<String, Object> body, String key, boolean defaultValue) {
        Object value = body.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        return "true".equalsIgnoreCase(String.valueOf(value)) || "Y".equalsIgnoreCase(String.valueOf(value));
    }
}
