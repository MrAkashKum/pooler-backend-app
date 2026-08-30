package com.akash.pooler_backend.service;

import com.akash.pooler_backend.entity.PbRefreshTokenEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.entity.PbUserSessionEntity;
import jakarta.servlet.http.HttpServletRequest;

public interface TokenService {

    PbRefreshTokenEntity createRefreshToken(PbUserEntity user, HttpServletRequest req);
    PbUserSessionEntity createSessionToken(PbUserEntity user, HttpServletRequest req);
    PbRefreshTokenEntity validateRefreshToken(String token);
    void revokeAllUserTokens(PbUserEntity user);
    void revokeRefreshToken(String token);
    void cleanupExpiredTokens();
}
