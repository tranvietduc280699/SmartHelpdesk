package org.example.besmarthelpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.besmarthelpdesk.enums.RequestCategory;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassifyResponseDto {
    private RequestCategory category;
    private double confidence;
    private String reason;
}
