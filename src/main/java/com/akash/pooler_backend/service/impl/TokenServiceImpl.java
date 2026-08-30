package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.config.AppProperties;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.entity.PbRefreshTokenEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.entity.PbUserSessionEntity;
import com.akash.pooler_backend.enums.TokenStatus;
import com.akash.pooler_backend.exception.RefreshTokenExpiredException;
import com.akash.pooler_backend.exception.TokenInvalidException;
import com.akash.pooler_backend.repository.PbRefreshTokenRepository;
import com.akash.pooler_backend.repository.PbUserSessionRepository;
import com.akash.pooler_backend.security.JwtUtil;
import com.akash.pooler_backend.service.TokenService;
import com.akash.pooler_backend.utils.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * @author Akash Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final JwtUtil jwtUtil;
    private final AppProperties props;
    private final PbRefreshTokenRepository refreshTokenRepo;
    private final PbUserSessionRepository userSessionRepository;


    @Override
    @Transactional
    public PbRefreshTokenEntity createRefreshToken(PbUserEntity pbUserEntity, HttpServletRequest req) {
        String rawToken = jwtUtil.generateRefreshToken(pbUserEntity);
        PbRefreshTokenEntity token = PbRefreshTokenEntity.builder()
                .refreshToken(rawToken)
                .entityId(pbUserEntity.getEntityId())
                .status(TokenStatus.ACTIVE)
                .expiresAt(Instant.now().plusMillis(props.getJwt().getRefreshTokenExpiryMs()))
                .deviceId(RequestUtil.getDeviceId(req))
                .platform(RequestUtil.getPlatform(req))
                .appVersion(RequestUtil.getAppVersion(req))
                .ipAddress(RequestUtil.getClientIp(req))
                .build();
        return refreshTokenRepo.save(token);
    }

    @Override
    @Transactional
    public PbUserSessionEntity createSessionToken(PbUserEntity pbUserEntity, HttpServletRequest req) {
        String rawToken = jwtUtil.generateSessionToken(pbUserEntity);
        PbUserSessionEntity pbUserSessionEntity = PbUserSessionEntity.builder()
                .token(rawToken)
                .entityId(pbUserEntity.getEntityId())
                .status(TokenStatus.ACTIVE)
                .expiresAt(Instant.now().plusMillis(props.getJwt().getSessionTokenExpiryMs()))
                .lastAccessedAt(Instant.now())
                .deviceId(RequestUtil.getDeviceId(req))
                .platform(RequestUtil.getPlatform(req))
                .ipAddress(RequestUtil.getClientIp(req))
                .appVersion(RequestUtil.getClientIp(req))
                .build();
        return userSessionRepository.save(pbUserSessionEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PbRefreshTokenEntity validateRefreshToken(String token) {
        PbRefreshTokenEntity pbRefreshTokenEntity = refreshTokenRepo.findByRefreshToken(token)
                .orElseThrow(() -> new TokenInvalidException(ResponseMessages.REFRESH_TOKEN_NOT_FOUND));

        if (pbRefreshTokenEntity.isRevoked())  throw new TokenInvalidException(ResponseMessages.REFRESH_TOKEN_REVOKED);
        if (pbRefreshTokenEntity.isExpired())  throw new RefreshTokenExpiredException();
        return pbRefreshTokenEntity;
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(PbUserEntity pbUserEntity) {
        refreshTokenRepo.revokeAllByEntityId(pbUserEntity.getEntityId());
        userSessionRepository.revokeAllByEntityId(pbUserEntity.getEntityId());
        log.info("Revoked all tokens for userId={}", pbUserEntity.getEntityId());

    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepo.findByRefreshToken(token).ifPresent(rt -> {
            rt.setStatus(TokenStatus.REVOKED);
            refreshTokenRepo.save(rt);
        });

    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    public void cleanupExpiredTokens() {
        refreshTokenRepo.cleanupExpiredTokens();
        userSessionRepository.cleanupExpiredTokens();
        log.info("Expired token cleanup completed");

    }
}
