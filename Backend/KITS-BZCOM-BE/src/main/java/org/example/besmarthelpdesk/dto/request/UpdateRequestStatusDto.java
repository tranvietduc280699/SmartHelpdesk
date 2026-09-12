package org.example.besmarthelpdesk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.enums.RequestStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRequestStatusDto {

    @NotNull(message = MessageConstants.STATUS_NULL)
    private RequestStatus status;

    private String memo;
}
