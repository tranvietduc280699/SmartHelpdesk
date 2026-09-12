package org.example.besmarthelpdesk.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.dto.RefreshTokenData;
import org.example.besmarthelpdesk.service.TokenRevocationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TokenRevocationServiceImpl implements TokenRevocationService {

    private final Map<String, RefreshTokenData> tokenStore = new ConcurrentHashMap<>();

    @Override
    public void storeRefreshToken(String tokenHash, UUID userId, String deviceInfo, Duration ttl) {
        log.info("(storeRefreshToken) Storing tokenHash: {} for userId: {}, device: {}", tokenHash, userId, deviceInfo);
        long expiryTime = System.currentTimeMillis() + ttl.toMillis();
        RefreshTokenData data = RefreshTokenData.builder()
                .userId(userId)
                .deviceInfo(deviceInfo)
                .expiryTime(expiryTime)
                .build();
        tokenStore.put(tokenHash, data);
    }

    @Override
    public Optional<RefreshTokenData> validateAndRevokeRefreshToken(String tokenHash, UUID userId) {
        log.info("(validateAndRevokeRefreshToken) Validating and revoking tokenHash: {} for userId: {}", tokenHash, userId);
        RefreshTokenData data = tokenStore.remove(tokenHash); // Retrieve and immediately revoke
        if (data == null) {
            log.warn("(validateAndRevokeRefreshToken) Token hash not found in store");
            return Optional.empty();
        }
        if (System.currentTimeMillis() > data.getExpiryTime()) {
            log.warn("(validateAndRevokeRefreshToken) Token has expired");
            return Optional.empty();
        }
        if (!data.getUserId().equals(userId)) {
            log.warn("(validateAndRevokeRefreshToken) Token userId mismatch. Expected: {}, Found: {}", userId, data.getUserId());
            return Optional.empty();
        }
        return Optional.of(data);
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        log.info("(revokeAllForUser) Revoking all tokens for userId: {}", userId);
        tokenStore.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
    }

    // Clean expired tokens every hour (3600000 ms)
    @Scheduled(fixedRate = 3600000)
    public void cleanExpiredTokens() {
        log.info("(cleanExpiredTokens) Running scheduled cleanup for expired refresh tokens");
        long now = System.currentTimeMillis();
        int initialSize = tokenStore.size();
        tokenStore.entrySet().removeIf(entry -> now > entry.getValue().getExpiryTime());
        int removedCount = initialSize - tokenStore.size();
        log.info("(cleanExpiredTokens) Evicted {} expired tokens", removedCount);
    }
}
