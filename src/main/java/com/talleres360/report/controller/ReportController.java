package com.talleres360.report.controller;

import com.talleres360.report.model.BusinessEvent;
import com.talleres360.report.service.ReportService;
import com.talleres360.report.service.ReportService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;

@RestController @RequiredArgsConstructor
public class ReportController {
    private final ReportService service;
    @Value("${app.internal-key}") private String internalKey;
    private void check(String candidate) {
        if (internalKey.isBlank() || candidate == null ||
                !MessageDigest.isEqual(internalKey.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial interna inválida");
    }
    @PostMapping("/internal/events") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ingest(@RequestHeader(value = "X-Internal-Key", required = false) String key, @Valid @RequestBody EventInput input) {
        check(key); service.ingest(input);
    }
    @GetMapping("/api/reports/sales")
    public SalesReport sales(@RequestHeader(value = "X-Internal-Key", required = false) String key, @RequestParam Instant from, @RequestParam Instant to) {
        check(key); return service.sales(from, to);
    }
    @GetMapping("/api/reports/audit")
    public List<BusinessEvent> audit(@RequestHeader(value = "X-Internal-Key", required = false) String key, @RequestParam(required = false) Long orderId) {
        check(key); return service.audit(orderId);
    }
}
