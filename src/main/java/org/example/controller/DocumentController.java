package org.example.controller;


import org.example.dto.ApiResponse;
import org.example.service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DocumentController<DocumentReviewRequest> {
    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(value = "/applications/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(@PathVariable long id, @RequestParam(required = false) Long checklistItemId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Document uploaded.", service.upload(id, checklistItemId, file)));
    }

    @GetMapping("/applications/{id}/documents")
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable long id) {
        return ApiResponse.ok("Application documents.", service.list(id));
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable long id) {
        DocumentService.DownloadedDocument file = service.download(id);
        String safeName = file.fileName().replaceAll("[\\r\\n\\\"]", "_");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType())).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(safeName, StandardCharsets.UTF_8).build().toString()).body(file.resource());
    }

    @PutMapping("/documents/{id}/review")
    public ApiResponse<Void> review(@PathVariable long id, @RequestBody DocumentReviewRequest body) {
        service.review(id, body);
        return ApiResponse.ok("Document review saved.");
    }
}

