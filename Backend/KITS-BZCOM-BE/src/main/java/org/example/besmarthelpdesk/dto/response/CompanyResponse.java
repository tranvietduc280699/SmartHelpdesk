package org.example.besmarthelpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CompanyResponse {
    private String companyId;
    private String companyName;
    private String address;
    private String phone;
}
