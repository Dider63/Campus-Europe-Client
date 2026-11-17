package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService service;
    public ProfileController(ProfileService service) { this.service = service; }
    @GetMapping public ApiResponse<Map<String, Object>> profile() { return ApiResponse.ok("Profile.", service.get()); }
    @PutMapping public ApiResponse<Map<String, Object>> update(@RequestBody ProfileUpdateRequest body) {
        return ApiResponse.ok("Profile updated.", service.update(body));
    }
}