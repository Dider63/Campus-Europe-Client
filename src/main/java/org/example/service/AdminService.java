package org.example.service;

import org.example.repository.AdminRepository;

import java.util.Set;

public class AdminService {
    private static final Set<String> STAFF_ROLES = Set.of("AGENCY_STAFF", "CONSULTANT", "ADMIN");
    private static final Set<String> ACCOUNT_STATUSES = Set.of("ACTIVE", "INACTIVE", "LOCKED", "PENDING");



}
