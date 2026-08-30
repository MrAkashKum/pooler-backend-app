package com.akash.pooler_backend.security;

public record AppleIdentityClaims(
        String subject,
        String email,
        boolean emailVerified
) {
}
