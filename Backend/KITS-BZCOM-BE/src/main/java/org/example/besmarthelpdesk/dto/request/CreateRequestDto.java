package org.example.besmarthelpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestPriority;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRequestDto {

    @NotBlank(message = MessageConstants.TITLE_BLANK)
    private String title;

    private String description;

    @NotNull(message = MessageConstants.CATEGORY_NULL)
    private RequestCategory category;

    @Builder.Default
    private RequestPriority priority = RequestPriority.MEDIUM;
}
