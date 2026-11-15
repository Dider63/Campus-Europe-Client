package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/countries")
    public ApiResponse<List<Map<String, Object>>> countries() {
        return ApiResponse.ok("Countries.", service.allCountries());
    }

    @PostMapping("/countries")
    public ResponseEntity<ApiResponse<Map<String, Object>>> country(@RequestBody CountryRequest b) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Country created.", Map.of("countryId", service.createCountry(b))));
    }

    @PutMapping("/countries/{id}")
    public ApiResponse<Void> country(@PathVariable long id, @RequestBody CountryRequest b) {
        service.updateCountry(id, b);
        return ApiResponse.ok("Country updated.");
    }

    @GetMapping("/visa-types")
    public ApiResponse<List<Map<String, Object>>> visaTypes() {
        return ApiResponse.ok("Visa types.", service.allVisaTypes());
    }

    @PostMapping("/visa-types")
    public ResponseEntity<ApiResponse<Map<String, Object>>> visaType(@RequestBody VisaTypeRequest b) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Visa type created.", Map.of("visaTypeId", service.createVisaType(b))));
    }

    @PutMapping("/visa-types/{id}")
    public ApiResponse<Void> visaType(@PathVariable long id, @RequestBody VisaTypeRequest b) {
        service.updateVisaType(id, b);
        return ApiResponse.ok("Visa type updated.");
    }

    @GetMapping("/visa-rules")
    public ApiResponse<List<Map<String, Object>>> visaRules() {
        return ApiResponse.ok("Visa rules.", service.visaRules());
    }

    @PostMapping("/visa-rules")
    public ResponseEntity<ApiResponse<Map<String, Object>>> visaRule(@RequestBody VisaRuleRequest b) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Visa rule created.", Map.of("visaRuleId", service.createVisaRule(b))));
    }

    @PutMapping("/visa-rules/{id}")
    public ApiResponse<Void> visaRule(@PathVariable long id, @RequestBody VisaRuleRequest b) {
        service.updateVisaRule(id, b);
        return ApiResponse.ok("Visa rule updated.");
    }

    @PatchMapping("/visa-rules/{id}/active")
    public ApiResponse<Void> active(@PathVariable long id, @RequestBody VisaRuleActivationRequest b) {
        service.updateRuleActive(id, b);
        return ApiResponse.ok("Visa rule activation updated.");
    }

    @PutMapping("/visa-rules/{id}/fee")
    public ApiResponse<Void> fee(@PathVariable long id, @RequestBody VisaRuleFeeRequest b) {
        service.updateFee(id, b);
        return ApiResponse.ok("Visa rule fee updated using update_visa_rule_fee.");
    }

    @GetMapping("/visa-rules/{id}/fee-history")
    public ApiResponse<List<Map<String, Object>>> feeHistory(@PathVariable long id) {
        return ApiResponse.ok("Fee change history.", service.feeHistory(id));
    }

    @PostMapping("/visa-rules/{id}/checklist")
    public ApiResponse<Void> attachChecklist(@PathVariable long id, @RequestBody VisaRuleChecklistRequest b) {
        service.attachChecklist(id, b);
        return ApiResponse.ok("Checklist association saved.");
    }

    @DeleteMapping("/visa-rules/{id}/checklist/{itemId}")
    public ApiResponse<Void> detachChecklist(@PathVariable long id, @PathVariable long itemId) {
        service.detachChecklist(id, itemId);
        return ApiResponse.ok("Checklist association removed.");
    }

    @GetMapping("/checklist-items")
    public ApiResponse<List<Map<String, Object>>> checklistItems() {
        return ApiResponse.ok("Checklist items.", service.checklistItems());
    }

    @PostMapping("/checklist-items")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checklistItem(@RequestBody ChecklistItemRequest b) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Checklist item created.", Map.of("checklistItemId", service.createChecklistItem(b))));
    }

    @PutMapping("/checklist-items/{id}")
    public ApiResponse<Void> checklistItem(@PathVariable long id, @RequestBody ChecklistItemRequest b) {
        service.updateChecklistItem(id, b);
        return ApiResponse.ok("Checklist item updated.");
    }

    @GetMapping("/staff")
    public ApiResponse<List<Map<String, Object>>> staff() {
        return ApiResponse.ok("Staff accounts.", service.staff());
    }

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<Map<String, Object>>> staff(@RequestBody StaffRequest b) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Staff account created.", service.createStaff(b)));
    }

    @PutMapping("/staff/{userId}")
    public ApiResponse<Void> staff(@PathVariable long userId, @RequestBody StaffRequest b) {
        service.updateStaff(userId, b);
        return ApiResponse.ok("Staff account updated.");
    }

    @PutMapping("/staff/{userId}/roles")
    public ApiResponse<Void> roles(@PathVariable long userId, @RequestBody StaffRolesRequest b) {
        service.changeStaffRoles(userId, b);
        return ApiResponse.ok("Staff roles updated.");
    }

    @GetMapping("/universities")
    public ApiResponse<List<Map<String, Object>>> universities() {
        return ApiResponse.ok("Universities.", service.universities());
    }

    @PostMapping("/universities")
    public ResponseEntity<ApiResponse<Map<String, Object>>> university(@RequestBody UniversityRequest b) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("University created.", Map.of("universityId", service.createUniversity(b))));
    }

    @PutMapping("/universities/{id}")
    public ApiResponse<Void> university(@PathVariable long id, @RequestBody UniversityRequest b) {
        service.updateUniversity(id, b);
        return ApiResponse.ok("University updated.");
    }

    @GetMapping("/reports/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok("Dashboard report.", service.report());
    }
}