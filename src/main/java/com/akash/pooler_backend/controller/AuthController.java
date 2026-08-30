package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.*;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.AuthResponse;
import com.akash.pooler_backend.dto.response.TokenRefreshResponse;
import com.akash.pooler_backend.interceptors.annotation.RateLimit;
import com.akash.pooler_backend.service.AuthService;
import com.akash.pooler_backend.utils.RequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints — consumed by Android/iOS Kotlin clients.
 *
 * All responses use the ApiResponse<T> envelope:
 * { success, message, errorCode, data, timestamp }
 *
 * Mobile clients should:
 * 1. Send X-Device-Id, X-Platform, X-App-Version headers
 * 2. Store access + refresh + session tokens securely (e.g. Android Keystore)
 * 3. Refresh access token before expiry using /auth/refresh
 */
@RestController
@RequestMapping(value = ApiMapping.AUTH_API)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token management, password reset")
public class AuthController {

    private final AuthService authService;

    // ── Register ─────────────────────────────────────────────────────

    @PostMapping(ApiMapping.REGISTER)
    @RateLimit(maxRequests = 5, windowSeconds = 300)
    @Operation(summary = "Register new user", description = "Creates a pending account and sends an email verification link.")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest req,
            HttpServletRequest httpReq) {
        authService.register(req, httpReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.message(ResponseMessages.SIGNUP_CREATED));
    }

    @PostMapping(ApiMapping.VERIFY_EMAIL)
    @RateLimit(maxRequests = 8, windowSeconds = 300)
    @Operation(summary = "Verify email", description = "Activates a pending email/password account.")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.EMAIL_VERIFIED));
    }

    @PostMapping(ApiMapping.RESEND_VERIFICATION)
    @RateLimit(maxRequests = 3, windowSeconds = 600)
    @Operation(summary = "Resend email verification", description = "Resends activation email when a pending account exists.")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest req,
            HttpServletRequest httpReq) {
        authService.resendVerification(req, httpReq);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.VERIFICATION_LINK_SENT_IF_PENDING));
    }

    // ── Login ─────────────────────────────────────────────────────────

    @PostMapping(ApiMapping.LOGIN)
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    @Operation(summary = "Login", description = "Returns access token (15m), refresh token (7d), session token (30m).")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req, httpReq)));
    }

    @PostMapping(ApiMapping.GOOGLE)
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    @Operation(summary = "Google sign-in", description = "Verifies a Google ID token and returns Pooler mobile tokens.")
    public ResponseEntity<ApiResponse<AuthResponse>> google(
            @Valid @RequestBody GoogleAuthRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.ok(ApiResponse.ok(authService.loginWithGoogle(req, httpReq)));
    }

    @PostMapping(ApiMapping.APPLE)
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    @Operation(summary = "Apple sign-in", description = "Verifies an Apple identity token and returns Hoppo mobile tokens.")
    public ResponseEntity<ApiResponse<AuthResponse>> apple(
            @Valid @RequestBody AppleAuthRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.ok(ApiResponse.ok(authService.loginWithApple(req, httpReq)));
    }

    // ── Refresh Token ─────────────────────────────────────────────────

    @PostMapping(ApiMapping.REFRESH)
    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token for a new access + refresh token pair.")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(req)));
    }

    // ── Logout ────────────────────────────────────────────────────────

    @PostMapping(ApiMapping.LOGOUT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout from current device")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpReq) {
        String token = RequestUtil.extractBearerToken(httpReq);
        authService.logout(token, httpReq);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.LOGGED_OUT));
    }

    @PostMapping(ApiMapping.LOGOUT_ALL)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout from ALL devices", description = "Revokes all tokens for the authenticated user.")
    public ResponseEntity<ApiResponse<Void>> logoutAll(HttpServletRequest httpReq) {
        String token = RequestUtil.extractBearerToken(httpReq);
        authService.logoutAll(token);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.LOGGED_OUT_ALL));
    }

    // ── Password Reset ────────────────────────────────────────────────

    @PostMapping(ApiMapping.FORGOT_PASSWORD)
    @RateLimit(maxRequests = 3, windowSeconds = 600)
    @Operation(summary = "Request password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req,
            HttpServletRequest httpReq) {
        authService.forgotPassword(req, httpReq);
        // Always 200 — never reveal whether email exists
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.RESET_LINK_SENT_IF_REGISTERED));
    }

    @PostMapping(ApiMapping.RESET_PASSWORD)
    @RateLimit(maxRequests = 5, windowSeconds = 300)
    @Operation(summary = "Complete password reset with token from email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.PASSWORD_RESET_SUCCESS));
    }
}
