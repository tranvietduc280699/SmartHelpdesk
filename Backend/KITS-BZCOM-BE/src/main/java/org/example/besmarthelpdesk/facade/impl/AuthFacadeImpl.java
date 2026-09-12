package org.example.besmarthelpdesk.facade.impl;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.dto.RefreshTokenData;
import org.example.besmarthelpdesk.dto.request.LoginRequest;
import org.example.besmarthelpdesk.dto.request.RegisterRequest;
import org.example.besmarthelpdesk.dto.request.TokenRefreshRequest;
import org.example.besmarthelpdesk.dto.response.LoginAuthResponse;
import org.example.besmarthelpdesk.dto.response.MemberResponse;
import org.example.besmarthelpdesk.dto.response.RefreshTokenResponse;
import org.example.besmarthelpdesk.enums.ErrorCode;
import org.example.besmarthelpdesk.exception.BaseException;
import org.example.besmarthelpdesk.facade.AuthFacade;
import org.example.besmarthelpdesk.security.JwtTokenProvider;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.example.besmarthelpdesk.service.MemberService;
import org.example.besmarthelpdesk.service.TokenRevocationService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFacadeImpl implements AuthFacade {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final MemberService memberService;
    private final TokenRevocationService tokenRevocationService;
    private final HttpServletRequest httpServletRequest;

    @Override
    public LoginAuthResponse login(LoginRequest request) {
        log.info("(login) request: {}", request);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = tokenProvider.generateAccessToken(userPrincipal.getUsername(), userPrincipal.getId());
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getUsername(), userPrincipal.getId());

        Claims refreshTokenClaims = tokenProvider.extractClaims(refreshToken);
        String tokenHash = refreshTokenClaims.getId();

        String deviceInfo = httpServletRequest.getHeader("User-Agent");
        if (deviceInfo == null || deviceInfo.isBlank()) {
            deviceInfo = "UNKNOWN_DEVICE";
        }

        Duration ttl = Duration.ofMillis(tokenProvider.getRefreshTokenExpiration());
        tokenRevocationService.storeRefreshToken(tokenHash, userPrincipal.getId(), deviceInfo, ttl);

        MemberResponse memberResponse = memberService.getMemberById(userPrincipal.getId());
        LoginAuthResponse.UserInfo userInfo = LoginAuthResponse.UserInfo.builder()
                .id(memberResponse.getId())
                .email(memberResponse.getEmail())
                .name(memberResponse.getName())
                .role(memberResponse.getRole())
                .phone(memberResponse.getPhone())
                .companyId(memberResponse.getCompanyId())
                .companyName(memberResponse.getCompanyName())
                .status(memberResponse.getStatus())
                .build();

        log.info("(login) token generated successfully for: {}", request.getEmail());
        return LoginAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userInfo)
                .build();
    }

    @Override
    public void logout() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserPrincipal userPrincipal) {
                log.info("(logout) userId: {}", userPrincipal.getId());
                tokenRevocationService.revokeAllForUser(userPrincipal.getId());
            } else {
                log.info("(logout)");
            }
        } else {
            log.info("(logout)");
        }
        SecurityContextHolder.clearContext();
    }

    @Override
    public MemberResponse register(RegisterRequest request) {
        log.info("(register) request: {}", request);
        return memberService.createMember(request);
    }

    @Override
    public RefreshTokenResponse refreshToken(TokenRefreshRequest request, String deviceInfo) {
        log.info("(refreshToken) Received refresh token request");

        if (deviceInfo == null || deviceInfo.isBlank()) {
            deviceInfo = "UNKNOWN_DEVICE";
        }
        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            log.warn("(refreshToken) Refresh token is invalid or expired");
            throw new BaseException(ErrorCode.AUTH_UNAUTHORIZED, MessageConstants.REFRESH_TOKEN_INVALID);
        }

        Claims claims = tokenProvider.extractClaims(refreshToken);
        String tokenHash = claims.getId();
        String userIdStr = claims.get("id", String.class);
        if (userIdStr == null || userIdStr.isBlank()) {
            log.warn("(refreshToken) userId claim is blank");
            throw new BaseException(ErrorCode.AUTH_UNAUTHORIZED, MessageConstants.REFRESH_TOKEN_INVALID);
        }

        UUID userId = UUID.fromString(userIdStr);

        Optional<RefreshTokenData> refreshTokenData =
                tokenRevocationService.validateAndRevokeRefreshToken(tokenHash, userId);
        if (refreshTokenData.isEmpty()) {
            log.warn("(refreshToken) Refresh token hash not found in revocation store (possible replay attack!)");
            throw new BaseException(ErrorCode.AUTH_UNAUTHORIZED, MessageConstants.REFRESH_TOKEN_INVALID);
        }

        String newAccessToken = tokenProvider.generateAccessToken(claims.getSubject(), userId);
        String newRefreshToken = tokenProvider.generateRefreshToken(claims.getSubject(), userId);

        Claims newRefreshTokenClaims = tokenProvider.extractClaims(newRefreshToken);
        String newRefreshTokenHash = newRefreshTokenClaims.getId();
        Duration ttl = Duration.ofMillis(tokenProvider.getRefreshTokenExpiration());

        tokenRevocationService.storeRefreshToken(newRefreshTokenHash, userId, deviceInfo, ttl);

        log.info("(refreshToken) Successfully rotated token for userId: {}", userId);
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenTtl(tokenProvider.getAccessTokenExpiration())
                .refreshTokenTtl(tokenProvider.getRefreshTokenExpiration())
                .build();
    }
}
