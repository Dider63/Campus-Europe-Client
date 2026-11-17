package org.example.controller;


import org.example.dto.ApiResponse;
import org.example.service.UniversityService;
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
public class UniversityController {
    private final UniversityService service;
    public UniversityController(UniversityService service) { this.service = service; }

    @GetMapping("/academic-records")
    public ApiResponse<List<Map<String, Object>>> academicRecords() {
        return ApiResponse.ok("Academic records.", service.academicRecords());
    }

    @PostMapping("/academic-records")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAcademic(@RequestBody AcademicRecordRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Academic record created.", service.createAcademicRecord(body)));
    }

    @PutMapping("/academic-records/{id}")
    public ApiResponse<Void> updateAcademic(@PathVariable long id, @RequestBody AcademicRecordRequest body) {
        service.updateAcademicRecord(id, body);
        return ApiResponse.ok("Academic record updated.");
    }

    @GetMapping("/staff/applicants/{applicantId}/academic-records")
    public ApiResponse<List<Map<String, Object>>> applicantAcademic(@PathVariable long applicantId) {
        return ApiResponse.ok("Applicant academic records.", service.academicRecordsForStaff(applicantId));
    }

    @GetMapping("/universities")
    public ApiResponse<List<Map<String, Object>>> universities(@RequestParam(required = false) Long countryId) {
        return ApiResponse.ok("University directory.", service.universities(countryId));
    }

    @GetMapping("/university-recommendations")
    public ApiResponse<List<Map<String, Object>>> recommendations(@RequestParam(required = false) Long applicantId) {
        return ApiResponse.ok("University recommendations.", service.recommendations(applicantId));
    }

    @PostMapping("/university-recommendations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRecommendation(@RequestBody UniversityRecommendationRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Staff-assisted recommendation created.", service.createRecommendation(body)));
    }
}

