package org.example.besmarthelpdesk.facade;

import org.example.besmarthelpdesk.dto.request.LoginRequest;
import org.example.besmarthelpdesk.dto.request.RegisterRequest;
import org.example.besmarthelpdesk.dto.request.TokenRefreshRequest;
import org.example.besmarthelpdesk.dto.response.LoginAuthResponse;
import org.example.besmarthelpdesk.dto.response.MemberResponse;
import org.example.besmarthelpdesk.dto.response.RefreshTokenResponse;

public interface AuthFacade {
    LoginAuthResponse login(LoginRequest request);
    void logout();
    MemberResponse register(RegisterRequest request);
    RefreshTokenResponse refreshToken(TokenRefreshRequest request, String deviceInfo);
}
