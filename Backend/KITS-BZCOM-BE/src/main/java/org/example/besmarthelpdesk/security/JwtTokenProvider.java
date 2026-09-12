package org.example.besmarthelpdesk.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationInMs;

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(userPrincipal.getUsername(), userPrincipal.getId());
    }

    public String generateAccessToken(String email, UUID userId) {
        log.info("(generateAccessToken) Generating for email: {}, userId: {}", email, userId);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("id", userId.toString())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String email, UUID userId) {
        log.info("(generateRefreshToken) Generating for email: {}, userId: {}", email, userId);
        Date now = new Date();
        long refreshTokenExpirationInMs = getRefreshTokenExpiration();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationInMs);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString()) // jti (tokenHash)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("id", userId.toString())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsernameFromJwt(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token status: {}", ex.getMessage());
        }
        return false;
    }

    public long getAccessTokenExpiration() {
        return jwtExpirationInMs;
    }

    public long getRefreshTokenExpiration() {
        return jwtExpirationInMs * 7;
    }
}
