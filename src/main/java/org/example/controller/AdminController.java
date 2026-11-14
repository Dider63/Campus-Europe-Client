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
        return ApiResponse.ok();
    }

    @PostMapping("/countries")
    public ResponseEntity<ApiResponse<Map<String, Object>>> country(@RequestBody) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Country created.", Map.of("countryId", service.createCountry(b))));
    }

    @PutMapping("/countries/{id}")
    public ApiResponse<Void> country(@PathVariable long id) {
          }
}