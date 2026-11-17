package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.service.WorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WorkflowController {
    private final WorkflowService service;
    public WorkflowController(WorkflowService service) { this.service = service; }

    @GetMapping("/applications/{id}/payments")
    public ApiResponse<List<Map<String, Object>>> payments(@PathVariable long id) {
        return ApiResponse.ok("Payment records.", service.payments(id));
    }

    @PostMapping("/applications/{id}/payments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payment(@PathVariable long id,
                                                                    @RequestBody PaymentRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Manual payment record created.", service.recordPayment(id, body)));
    }

    @PutMapping("/payments/{id}")
    public ApiResponse<Map<String, Object>> updatePayment(@PathVariable long id, @RequestBody PaymentRequest body) {
        return ApiResponse.ok("Payment record updated.", service.updatePayment(id, body));
    }

    @GetMapping("/payments/{id}/invoice")
    public ApiResponse<Map<String, Object>> invoice(@PathVariable long id) {
        return ApiResponse.ok("Invoice details (printable from the browser).", service.invoice(id));
    }

    @GetMapping("/appointments/integration")
    public ApiResponse<Map<String, Object>> meetingIntegration() {
        return ApiResponse.ok("Google Meet integration status.", service.meetingIntegration());
    }

    @GetMapping("/appointments")
    public ApiResponse<List<Map<String, Object>>> appointments() { return ApiResponse.ok("Consultation appointments.", service.appointments()); }

    @PostMapping("/appointments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> appointment(@RequestBody AppointmentRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Appointment request created.", service.requestAppointment(body)));
    }

    @PutMapping("/appointments/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable long id, @RequestBody AppointmentConfirmationRequest body) {
        service.confirmAppointment(id, body);
        return ApiResponse.ok("Appointment confirmed.");
    }

    @PutMapping("/appointments/{id}/complete")
    public ApiResponse<Void> complete(@PathVariable long id) {
        service.completeAppointment(id);
        return ApiResponse.ok("Appointment marked completed.");
    }

    @PutMapping("/appointments/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable long id) {
        service.cancelAppointment(id);
        return ApiResponse.ok("Appointment cancelled.");
    }
}

