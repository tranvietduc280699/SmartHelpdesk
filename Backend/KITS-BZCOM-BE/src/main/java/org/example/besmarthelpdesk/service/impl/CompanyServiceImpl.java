package org.example.besmarthelpdesk.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.dto.response.CompanyResponse;
import org.example.besmarthelpdesk.entity.Company;
import org.example.besmarthelpdesk.exception.ResourceNotFoundException;
import org.example.besmarthelpdesk.repository.CompanyRepository;
import org.example.besmarthelpdesk.service.CompanyService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;

    @Override
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll(Sort.by("companyName", "id")).stream()
                .map(this::mapToResponse).toList();
    }

    @Override
    public CompanyResponse getCompanyById(String companyId) {
        return companyRepository.findById(companyId).map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.COMPANY_NOT_FOUND + companyId));
    }

    private CompanyResponse mapToResponse(Company company) {
        return new CompanyResponse(company.getId(), company.getCompanyName(), company.getAddress(), company.getPhone());
    }
}
