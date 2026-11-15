package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApplicationController {
    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping("/applications")
    public ResponseEntity<ApiResponse<ApplicationCreatedResponse>> create(@RequestBody ApplicationCreateRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Draft application created.", ApplicationCreatedResponse.from(service.create(body))));
    }

    @GetMapping("/applications")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok("Applications.", service.list());
    }

    @GetMapping("/applications/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable long id) {
        return ApiResponse.ok("Application details.", service.get(id));
    }

    @PutMapping("/applications/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody ApplicationDetailsRequest body) {
        service.updateDetails(id, body);
        return ApiResponse.ok("Draft application details saved.");
    }

    @PostMapping("/applications/{id}/submit")
    public ApiResponse<Void> submit(@PathVariable long id) {
        service.submit(id);
        return ApiResponse.ok("Application submitted. Oracle checklist validation was applied.");
    }

    @GetMapping("/applications/{id}/status-history")
    public ApiResponse<List<Map<String, Object>>> history(@PathVariable long id) {
        return ApiResponse.ok("Application status history.", service.history(id));
    }

    @GetMapping("/staff/applications")
    public ApiResponse<List<Map<String, Object>>> staffSearch(@RequestParam(required = false) String status,
                                                              @RequestParam(required = false) String q) {
        return ApiResponse.ok("Staff application search.", service.searchStaff(status, q));
    }

    @PutMapping("/staff/applications/{id}/status")
    public ApiResponse<Void> status(@PathVariable long id, @RequestBody ApplicationStatusRequest body) {
        service.setStatus(id, body);
        return ApiResponse.ok("Application status updated using set_visa_app_status.");
    }
}
