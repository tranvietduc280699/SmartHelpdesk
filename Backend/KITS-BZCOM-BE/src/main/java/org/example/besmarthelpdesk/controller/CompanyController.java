package org.example.besmarthelpdesk.controller;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.besmarthelpdesk.dto.ResponseGeneral;
import org.example.besmarthelpdesk.dto.response.CompanyResponse;
import org.example.besmarthelpdesk.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Read-only company directory; authentication required")
public class CompanyController {
    private final CompanyService companyService;

    @GetMapping
    @Operation(summary = "List all companies", description = "Sorted by company name, then company ID")
    public ResponseEntity<ResponseGeneral<List<CompanyResponse>>> getAllCompanies() {
        return ResponseEntity.ok(ResponseGeneral.success(companyService.getAllCompanies()));
    }

    @GetMapping("/{companyId}")
    @Operation(summary = "Get company details", description = "Returns 404 if the company does not exist")
    public ResponseEntity<ResponseGeneral<CompanyResponse>> getCompany(@PathVariable String companyId) {
        return ResponseEntity.ok(ResponseGeneral.success(companyService.getCompanyById(companyId)));
    }
}
