package org.example.besmarthelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.dto.ResponseGeneral;
import org.example.besmarthelpdesk.dto.request.LoginRequest;
import org.example.besmarthelpdesk.dto.request.RegisterRequest;
import org.example.besmarthelpdesk.dto.request.TokenRefreshRequest;
import org.example.besmarthelpdesk.dto.response.LoginAuthResponse;
import org.example.besmarthelpdesk.dto.response.MemberResponse;
import org.example.besmarthelpdesk.dto.response.RefreshTokenResponse;
import org.example.besmarthelpdesk.facade.AuthFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthFacade authFacade;

    @PostMapping("/login")
    public ResponseEntity<ResponseGeneral<LoginAuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("(login) request: {}", request);
        LoginAuthResponse response = authFacade.login(request);
        ResponseGeneral<LoginAuthResponse> body = ResponseGeneral.success(MessageConstants.LOGIN_SUCCESS, response);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseGeneral<Void>> logout() {
        log.info("(logout)");
        authFacade.logout();
        ResponseGeneral<Void> body = ResponseGeneral.success(MessageConstants.LOGOUT_SUCCESS, null);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseGeneral<MemberResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("(register) request: {}", request);
        MemberResponse response = authFacade.register(request);
        ResponseGeneral<MemberResponse> body = ResponseGeneral.success(MessageConstants.REGISTER_SUCCESS, response);
        return new ResponseEntity<>(body, HttpStatus.CREATED);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseGeneral<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request,
            @RequestHeader(value = "User-Agent", required = false) String deviceInfo) {
        log.info("(refreshToken) request: {}", request);
        RefreshTokenResponse response = authFacade.refreshToken(request, deviceInfo);
        ResponseGeneral<RefreshTokenResponse> body = ResponseGeneral.success(MessageConstants.REFRESH_SUCCESS, response);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
