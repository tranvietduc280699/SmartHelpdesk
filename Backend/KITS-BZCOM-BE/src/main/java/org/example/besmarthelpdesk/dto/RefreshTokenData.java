package org.example.besmarthelpdesk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenData {
    private UUID userId;
    private String deviceInfo;
    private long expiryTime;
}
