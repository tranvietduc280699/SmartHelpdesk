package org.example.besmarthelpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendChatMessageRequest {

    @NotBlank
    @Size(max = 10000)
    private String message;

    @Size(max = 20)
    private String messageType = "TEXT";
}
