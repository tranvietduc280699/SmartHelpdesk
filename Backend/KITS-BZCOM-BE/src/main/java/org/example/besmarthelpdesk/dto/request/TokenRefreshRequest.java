package org.example.besmarthelpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.besmarthelpdesk.constant.MessageConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {

    @NotBlank(message = MessageConstants.REFRESH_TOKEN_BLANK)
    private String refreshToken;
}
