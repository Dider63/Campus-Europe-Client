package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.service.ApplicationService;
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
    public ApplicationController(ApplicationService service) { this.service = service; }

    @PostMapping("/applications")
    public ApiResponse<Object> create() {

    }

    @GetMapping("/applications")
    public ApiResponse<List<Map<String, Object>>> list()
    {

    }

    @GetMapping("/applications/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable long id)
    {

    }

    @PutMapping("/applications/{id}")
    public ApiResponse<Void> update(@PathVariable long id) {

    }

    @PostMapping("/applications/{id}/submit")
    public ApiResponse<Void> submit(@PathVariable long id) {
          }

    @GetMapping("/applications/{id}/status-history")
    public ApiResponse<List<Map<String, Object>>> history(@PathVariable long id) {

    }

    @GetMapping("/staff/applications")
    public ApiResponse<List<Map<String, Object>>> staffSearch() {

    }

    @PutMapping("/staff/applications/{id}/status")
    public ApiResponse<Void> status(@PathVariable long id) {

           }
}
