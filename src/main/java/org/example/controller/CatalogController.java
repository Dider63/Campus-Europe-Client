package org.example.controller;


import org.example.dto.ApiResponse;
import org.example.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @GetMapping("/countries")
    public ApiResponse<List<Map<String, Object>>> countries() {
        return ApiResponse.ok("Countries.", service.countries());
    }

    @GetMapping("/visa-types")
    public ApiResponse<List<Map<String, Object>>> visaTypes() {
        return ApiResponse.ok("Active visa types.", service.visaTypes());
    }

    @GetMapping("/visa-catalog")
    public ApiResponse<List<Map<String, Object>>> catalog(@RequestParam(required = false) Long destinationCountryId,
                                                          @RequestParam(required = false) Long nationalityCountryId,
                                                          @RequestParam(required = false) Long visaTypeId) {
        return ApiResponse.ok("Visa catalog.", service.catalog(destinationCountryId, nationalityCountryId, visaTypeId));
    }

    @GetMapping("/visa-rules/{id}")
    public ApiResponse<Map<String, Object>> rule(@PathVariable long id) {
        return ApiResponse.ok("Visa rule.", service.rule(id));
    }

    @GetMapping("/visa-rules/{id}/checklist")
    public ApiResponse<List<Map<String, Object>>> checklist(@PathVariable long id) {
        return ApiResponse.ok("Required and optional checklist items.", service.checklist(id));
    }
}
