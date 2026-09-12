package org.example.besmarthelpdesk.service;

import org.example.besmarthelpdesk.dto.RefreshTokenData;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface TokenRevocationService {
    void storeRefreshToken(String tokenHash, UUID userId, String deviceInfo, Duration ttl);
    Optional<RefreshTokenData> validateAndRevokeRefreshToken(String tokenHash, UUID userId);
    void revokeAllForUser(UUID userId);
}
