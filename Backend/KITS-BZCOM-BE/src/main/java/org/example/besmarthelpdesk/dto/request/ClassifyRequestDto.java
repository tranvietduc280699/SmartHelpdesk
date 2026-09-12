package org.example.besmarthelpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.besmarthelpdesk.constant.MessageConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassifyRequestDto {

    @NotBlank(message = MessageConstants.DESCRIPTION_BLANK)
    private String description;
}
