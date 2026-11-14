package org.example.service;



import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ApplicationService {
    private static final Set<String> STATUSES = Set.of("DRAFT", "SUBMITTED", "UNDER_REVIEW", "PROCESSING", "EMBASSY", "APPROVED", "REJECTED");
    private static final Map<String, Set<String>> STATUS_TRANSITIONS = Map.of(
            "SUBMITTED", Set.of("UNDER_REVIEW", "REJECTED"),
            "UNDER_REVIEW", Set.of("PROCESSING", "REJECTED"),
            "PROCESSING", Set.of("EMBASSY", "APPROVED", "REJECTED"),
            "EMBASSY", Set.of("PROCESSING", "APPROVED", "REJECTED"),
            "APPROVED", Set.of(),
            "REJECTED", Set.of(),
            "DRAFT", Set.of()
    );


    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
