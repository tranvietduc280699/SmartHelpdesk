package org.example.besmarthelpdesk.service;

import java.util.List;
import org.example.besmarthelpdesk.dto.response.CompanyResponse;

public interface CompanyService {
    List<CompanyResponse> getAllCompanies();
    CompanyResponse getCompanyById(String companyId);
}
