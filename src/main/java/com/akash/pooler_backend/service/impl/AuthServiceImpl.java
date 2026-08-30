package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.config.AppProperties;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.*;
import com.akash.pooler_backend.dto.response.AuthResponse;
import com.akash.pooler_backend.dto.response.TokenRefreshResponse;
import com.akash.pooler_backend.dto.response.UserResponse;
import com.akash.pooler_backend.entity.*;
import com.akash.pooler_backend.enums.Role;
import com.akash.pooler_backend.enums.TokenStatus;
import com.akash.pooler_backend.enums.UserStatus;
import com.akash.pooler_backend.exception.*;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbEntitySequenceRepository;
import com.akash.pooler_backend.repository.PbEmailVerificationTokenRepository;
import com.akash.pooler_backend.repository.PbPasswordResetTokenRepository;
import com.akash.pooler_backend.repository.PbRefreshTokenRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.security.AppleIdentityClaims;
import com.akash.pooler_backend.security.AppleIdentityTokenVerifier;
import com.akash.pooler_backend.security.JwtUtil;
import com.akash.pooler_backend.service.AuthService;
import com.akash.pooler_backend.service.MailService;
import com.akash.pooler_backend.service.TokenService;
import com.akash.pooler_backend.service.UserService;
import com.akash.pooler_backend.utils.RequestUtil;
import com.akash.pooler_backend.utils.SecureTokenUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Akash Kumar
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final String GOOGLE_ID_TOKEN_QUERY_PARAM = "id_token";
    private static final int ACCOUNT_TOKEN_BYTES = 32;
    private static final int SOCIAL_LOGIN_PASSWORD_BYTES = 48;

    private final AuthenticationManager authManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final MailService mailService;
    private final AppProperties props;
    private final PbUserRepository userRepo;
    private final PbRefreshTokenRepository refreshTokenRepo;
    private final PbPasswordResetTokenRepository resetTokenRepo;
    private final PbEmailVerificationTokenRepository emailVerificationTokenRepo;
    private final PbEntitySequenceRepository pbEntitySequenceRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final AppleIdentityTokenVerifier appleIdentityTokenVerifier;
    private HttpClient googleAuthHttpClient;

    @PostConstruct
    void initializeGoogleAuthHttpClient() {
        googleAuthHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(props.getAuth().getGoogle().getConnectTimeoutSeconds()))
                .build();
    }


    // ── Register ──────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction("USER_REGISTER")
    public void register(RegisterRequest req, HttpServletRequest httpReq) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException(ResponseMessages.PASSWORDS_DO_NOT_MATCH);
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new UserAlreadyExistsException(req.getEmail());
        }

        PbEntityIdSequence pbEntityIdSequence = pbEntitySequenceRepository.save(new PbEntityIdSequence());

        PbUserEntity pbUserEntity = PbUserEntity.builder()
                .email(req.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .entityId(Long.toString(pbEntityIdSequence.getId()))
                .username("user-" + pbEntityIdSequence.getId())
                .role(Role.ROLE_USER)
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .gender(req.getGender())
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        pbUserEntity = userRepo.save(pbUserEntity);
        String verificationToken = SecureTokenUtil.urlSafeToken(ACCOUNT_TOKEN_BYTES);
        emailVerificationTokenRepo.save(PbEmailVerificationTokenEntity.builder()
                .token(verificationToken)
                .entityId(pbUserEntity.getEntityId())
                .status(TokenStatus.ACTIVE)
                .expiresAt(Instant.now().plusSeconds(props.getEmailVerification().getTokenExpiryMinutes() * 60L))
                .requestedFromIp(RequestUtil.getClientIp(httpReq))
                .build());

        mailService.sendEmailVerificationMail(pbUserEntity, verificationToken);
        log.info("New user registered pending email verification: userId={}", pbUserEntity.getEntityId());
    }

    // ── Login ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction("USER_LOGIN")
    public AuthResponse login(LoginRequest req, HttpServletRequest httpReq) {
        PbUserEntity pbUserEntity = userRepo.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthenticationException(ResponseMessages.INVALID_CREDENTIALS));

        checkAccountStatus(pbUserEntity);

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (BadCredentialsException e) {
            handleFailedLogin(pbUserEntity);
            throw new AuthenticationException(ResponseMessages.INVALID_CREDENTIALS);
        }

        pbUserEntity.resetFailedAttempts();
        userRepo.updateLoginSuccess(pbUserEntity.getEntityId(), Instant.now());

        log.info("User logged in: userId={} platform={}", pbUserEntity.getEntityId(), req.getPlatform());
        return buildAuthResponse(pbUserEntity, httpReq);
    }

    @Override
    @Transactional
    @AuditAction("USER_GOOGLE_LOGIN")
    public AuthResponse loginWithGoogle(GoogleAuthRequest req, HttpServletRequest httpReq) {
        Map<String, String> claims = verifyGoogleIdToken(req.getIdToken());
        String email = claims.get("email").toLowerCase().trim();

        PbUserEntity user = userRepo.findByEmail(email).orElseGet(() -> {
            PbEntityIdSequence sequence = pbEntitySequenceRepository.save(new PbEntityIdSequence());
            String givenName = claims.getOrDefault("given_name", "Hoppo").trim();
            String familyName = claims.getOrDefault("family_name", "Rider").trim();
            return userRepo.save(PbUserEntity.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(SecureTokenUtil.urlSafeToken(SOCIAL_LOGIN_PASSWORD_BYTES)))
                    .entityId(Long.toString(sequence.getId()))
                    .username("user-" + sequence.getId())
                    .role(Role.ROLE_USER)
                    .firstName(givenName.isBlank() ? "Hoppo" : givenName)
                    .lastName(familyName.isBlank() ? "Rider" : familyName)
                    .profilePictureUrl(claims.get("picture"))
                    .status(UserStatus.ACTIVE)
                    .build());
        });

        checkAccountStatus(user);
        userRepo.updateLoginSuccess(user.getEntityId(), Instant.now());
        return buildAuthResponse(user, httpReq);
    }

    @Override
    @Transactional
    @AuditAction("USER_APPLE_LOGIN")
    public AuthResponse loginWithApple(AppleAuthRequest req, HttpServletRequest httpReq) {
        AppleIdentityClaims claims = appleIdentityTokenVerifier.verify(req.getIdentityToken());
        String email = claims.email();

        PbUserEntity user = userRepo.findByEmail(email).orElseGet(() -> {
            PbEntityIdSequence sequence = pbEntitySequenceRepository.save(new PbEntityIdSequence());
            String firstName = cleanName(req.getFirstName(), "Hoppo");
            String lastName = cleanName(req.getLastName(), "Rider");
            return userRepo.save(PbUserEntity.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(SecureTokenUtil.urlSafeToken(SOCIAL_LOGIN_PASSWORD_BYTES)))
                    .entityId(Long.toString(sequence.getId()))
                    .username("user-" + sequence.getId())
                    .role(Role.ROLE_USER)
                    .firstName(firstName)
                    .lastName(lastName)
                    .status(UserStatus.ACTIVE)
                    .build());
        });

        checkAccountStatus(user);
        userRepo.updateLoginSuccess(user.getEntityId(), Instant.now());
        log.info("User logged in with Apple: userId={}", user.getEntityId());
        return buildAuthResponse(user, httpReq);
    }

    // ── Refresh Token ─────────────────────────────────────────────────

    @Override
    @Transactional
    public TokenRefreshResponse refresh(RefreshTokenRequest req) {
        PbRefreshTokenEntity pbRefreshTokenEntity = tokenService.validateRefreshToken(req.getRefreshToken());

        String entityId = pbRefreshTokenEntity.getEntityId();
        PbUserEntity pbUserEntity = userService.getUserEntity(entityId);
        checkAccountStatus(pbUserEntity);

        // Rotate: revoke old, issue new
        pbRefreshTokenEntity.setStatus(TokenStatus.REVOKED);
        refreshTokenRepo.save(pbRefreshTokenEntity);

        String newAccessToken  = jwtUtil.generateAccessToken(pbUserEntity);
        String newRefreshToken = jwtUtil.generateRefreshToken(pbUserEntity);

        PbRefreshTokenEntity newRt = PbRefreshTokenEntity.builder()
                .refreshToken(newRefreshToken)
                .entityId(entityId)
                .status(TokenStatus.ACTIVE)
                .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTokenExpiryMs()))
                .deviceId(pbRefreshTokenEntity.getDeviceId())
                .platform(pbRefreshTokenEntity.getPlatform())
                .build();
        refreshTokenRepo.save(newRt);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresIn(props.getJwt().getAccessTokenExpiryMs() / 1000)
                .build();
    }

    // ── Logout ────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction("USER_LOGOUT")
    public void logout(String accessToken, HttpServletRequest httpReq) {
        String entityId = jwtUtil.extractSubject(accessToken);
        userRepo.findByEntityId(entityId).ifPresent(user -> {
            String deviceId = RequestUtil.getDeviceId(httpReq);
            refreshTokenRepo.revokeAllByEntityId(user.getEntityId());
            log.info("User logged out: userId={} deviceId={}", user.getEntityId(), deviceId);
        });

    }



    @Override
    @Transactional
    @AuditAction("USER_LOGOUT_ALL")
    public void logoutAll(String accessToken) {
        String entityId = jwtUtil.extractSubject(accessToken);
        userRepo.findByEntityId(entityId).ifPresent(user -> {
            tokenService.revokeAllUserTokens(user);
            log.info("All tokens revoked for userId={}", user.getEntityId());
        });

    }

    // ── Forgot Password ───────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction("PASSWORD_RESET_REQUEST")
    public void forgotPassword(ForgotPasswordRequest req, HttpServletRequest httpReq) {
        // Always return 200 — never leak whether email exists (security)
        userRepo.findByEmail(req.getEmail().toLowerCase()).ifPresent(user -> {
            resetTokenRepo.revokeAllByEntityId(user.getEntityId());

            String rawToken = SecureTokenUtil.urlSafeToken(ACCOUNT_TOKEN_BYTES);
            PbPasswordResetTokenEntity prt = PbPasswordResetTokenEntity.builder()
                    .token(rawToken)
                    .entityId(user.getEntityId())
                    .status(TokenStatus.ACTIVE)
                    .expiresAt(Instant.now().plusSeconds(
                            props.getPasswordReset().getTokenExpiryMinutes() * 60L))
                    .requestedFromIp(RequestUtil.getClientIp(httpReq))
                    .build();
            resetTokenRepo.save(prt);

            mailService.sendPasswordResetMail(user, rawToken);
            log.info("Password reset mail sent for userId={}", user.getEntityId());
        });
    }

    @Override
    @Transactional
    @AuditAction("EMAIL_VERIFY")
    public void verifyEmail(VerifyEmailRequest req) {
        PbEmailVerificationTokenEntity token = emailVerificationTokenRepo.findByToken(req.getToken())
                .orElseThrow(EmailVerificationInvalidException::new);
        if (!token.isValid()) {
            throw new EmailVerificationInvalidException();
        }

        PbUserEntity user = userService.getUserEntity(token.getEntityId());
        user.setStatus(UserStatus.ACTIVE);
        userRepo.save(user);

        token.setStatus(TokenStatus.USED);
        emailVerificationTokenRepo.save(token);

        mailService.sendWelcomeMail(user);
        log.info("Email verified for userId={}", user.getEntityId());
    }

    @Override
    @Transactional
    @AuditAction("EMAIL_VERIFICATION_RESEND")
    public void resendVerification(ResendVerificationRequest req, HttpServletRequest httpReq) {
        userRepo.findByEmail(req.getEmail().toLowerCase().trim())
                .filter(user -> user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(user -> {
                    emailVerificationTokenRepo.revokeAllByEntityId(user.getEntityId());
                    String verificationToken = SecureTokenUtil.urlSafeToken(ACCOUNT_TOKEN_BYTES);
                    emailVerificationTokenRepo.save(PbEmailVerificationTokenEntity.builder()
                            .token(verificationToken)
                            .entityId(user.getEntityId())
                            .status(TokenStatus.ACTIVE)
                            .expiresAt(Instant.now().plusSeconds(props.getEmailVerification().getTokenExpiryMinutes() * 60L))
                            .requestedFromIp(RequestUtil.getClientIp(httpReq))
                            .build());
                    mailService.sendEmailVerificationMail(user, verificationToken);
                    log.info("Email verification resent for userId={}", user.getEntityId());
                });
    }

    // ── Reset Password ────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditAction("PASSWORD_RESET_COMPLETE")
    public void resetPassword(ResetPasswordRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new InvalidResetTokenException(); // reuse — avoid leaking field name
        }

        PbPasswordResetTokenEntity pbPasswordResetTokenEntity = resetTokenRepo.findByToken(req.getToken())
                .orElseThrow(InvalidResetTokenException::new);

        if (!pbPasswordResetTokenEntity.isValid()) throw new InvalidResetTokenException();

        String entityId = pbPasswordResetTokenEntity.getEntityId();
        /* Add Validation for pbUserEntity if not valid */
        PbUserEntity pbUserEntity = userService.getUserEntity(entityId);

        pbUserEntity.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(pbUserEntity);

        pbPasswordResetTokenEntity.setStatus(TokenStatus.USED);
        resetTokenRepo.save(pbPasswordResetTokenEntity);

        tokenService.revokeAllUserTokens(pbUserEntity); // force re-login on all devices
        log.info("Password reset completed for userId={}", pbUserEntity.getEntityId());
    }

    // ── Private helpers ───────────────────────────────────────────────

    private AuthResponse buildAuthResponse(PbUserEntity pbUserEntity, HttpServletRequest req) {
        String accessToken = jwtUtil.generateAccessToken(pbUserEntity);
        PbRefreshTokenEntity pbRefreshTokenEntity = tokenService.createRefreshToken(pbUserEntity, req);
        PbUserSessionEntity pbUserSessionEntity = tokenService.createSessionToken(pbUserEntity, req);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(pbRefreshTokenEntity.getRefreshToken())
                .sessionToken(pbUserSessionEntity.getToken())
                .accessTokenExpiresIn(props.getJwt().getAccessTokenExpiryMs() / 1000)
                .refreshTokenExpiresIn(props.getJwt().getRefreshTokenExpiryMs() / 1000)
                .user(UserResponse.from(pbUserEntity))
                .build();
    }

    private Map<String, String> verifyGoogleIdToken(String idToken) {
        AppProperties.Auth.Google google = props.getAuth().getGoogle();
        String googleClientIds = google.getClientIds();
        if (googleClientIds == null || googleClientIds.isBlank()) {
            throw new AuthenticationException(ResponseMessages.GOOGLE_SIGN_IN_NOT_CONFIGURED);
        }
        try {
            String token = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildGoogleTokenInfoUri(google.getTokenInfoUrl(), token))
                    .timeout(Duration.ofSeconds(google.getRequestTimeoutSeconds()))
                    .GET()
                    .build();
            HttpResponse<String> response = googleAuthHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AuthenticationException(ResponseMessages.GOOGLE_ID_TOKEN_INVALID);
            }

            Map<String, String> claims = objectMapper.readValue(response.body(), new TypeReference<>() {});
            boolean acceptedAudience = Arrays.stream(googleClientIds.split(","))
                    .map(String::trim)
                    .anyMatch(clientId -> !clientId.isBlank() && clientId.equals(claims.get("aud")));
            boolean verifiedEmail = "true".equalsIgnoreCase(claims.get("email_verified"));
            long expiresAt = Long.parseLong(claims.getOrDefault("exp", "0"));
            if (!acceptedAudience || !verifiedEmail || claims.get("email") == null
                    || expiresAt <= Instant.now().getEpochSecond()) {
                throw new AuthenticationException(ResponseMessages.GOOGLE_TOKEN_VALIDATION_FAILED);
            }
            return claims;
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("googleTokenVerificationInterrupted className={} methodName={}",
                    getClass().getSimpleName(), "verifyGoogleToken");
            throw new AuthenticationException(ResponseMessages.GOOGLE_SIGN_IN_VERIFY_FAILED);
        } catch (Exception exception) {
            log.warn("googleTokenVerificationFailed className={} methodName={} exceptionType={}",
                    getClass().getSimpleName(), "verifyGoogleToken", exception.getClass().getSimpleName());
            throw new AuthenticationException(ResponseMessages.GOOGLE_SIGN_IN_VERIFY_FAILED);
        }
    }

    private URI buildGoogleTokenInfoUri(String tokenInfoUrl, String encodedIdToken) {
        String separator = tokenInfoUrl.contains("?") ? "&" : "?";
        return URI.create(tokenInfoUrl + separator + GOOGLE_ID_TOKEN_QUERY_PARAM + "=" + encodedIdToken);
    }

    private static String cleanName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void checkAccountStatus(PbUserEntity user) {
        if (user.getStatus() == UserStatus.SUSPENDED) throw new AccountSuspendedException();
        if (!user.isAccountNonLocked()) throw new AccountLockedException();
        if (!user.isEnabled()) throw new AuthenticationException(ResponseMessages.ACCOUNT_NOT_ACTIVE);
    }

    private void handleFailedLogin(PbUserEntity pbUserEntity) {
        pbUserEntity.incrementFailedAttempts();
        int max = props.getSecurity().getMaxFailedAttempts();
        if (pbUserEntity.getFailedLoginAttempts() >= max) {
            pbUserEntity.setLockedUntil(Instant.now().plusSeconds(
                    props.getSecurity().getLockDurationMinutes() * 60L));
            pbUserEntity.setStatus(UserStatus.LOCKED);
            userRepo.save(pbUserEntity);
            mailService.sendAccountLockedMail(pbUserEntity);
            log.warn("Account locked after {} failed attempts: userId={}", max, pbUserEntity.getEntityId());
            throw new AccountLockedException(ResponseMessages.accountLockedAfterAttempts(max));
        }
        userRepo.save(pbUserEntity);
    }
}
