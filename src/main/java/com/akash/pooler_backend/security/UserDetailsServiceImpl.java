package com.akash.pooler_backend.security;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.UserStatus;
import com.akash.pooler_backend.repository.PbUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  UserDetailsServiceImpl                                          │
 * │                                                                  │
 * │  Spring Security's core user-loading contract.                  │
 * │  Called by:                                                      │
 * │    • DaoAuthenticationProvider (during login)                   │
 * │    • JwtAuthenticationFilter (on every authenticated request)   │
 * │                                                                  │
 * │  Account status checks:                                          │
 * │    LOCKED    → LockedException   → 403 ACCOUNT_LOCKED           │
 * │    SUSPENDED → DisabledException → 403 ACCOUNT_SUSPENDED        │
 * │    INACTIVE  → DisabledException → 403 ACCOUNT_INACTIVE         │
 * │    NOT FOUND → UsernameNotFoundException → 401                  │
 * │                                                                  │
 * │  NOTE: The User entity implements UserDetails directly, so no   │
 * │  adaptor is needed — keeps the design simple and type-safe.     │
 * └─────────────────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final PbUserRepository userRepository;

    /**
     * Load PbUserEntity by email/username (email/username is the username in this system).
     *
     * @param -username/email the user's email address
     * @return UserDetails (backed by the PbUserEntity JPA entity)
     * @throws UsernameNotFoundException if no PbUserEntity exists with this email/username
     * @throws LockedException if the account is currently locked
     * @throws DisabledException if the account is suspended or inactive
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Loading user details for authentication request");

        /* Email validation support only, but later we can give support for username also */
        PbUserEntity pbUserEntity = userRepository
                .findByEmail(usernameOrEmail.toLowerCase().trim())
                .orElseThrow(() -> {
                    log.warn("UserDetailsService: user not found for authentication request");
                    // Use generic message — never confirm whether email exists
                    return new UsernameNotFoundException(ResponseMessages.INVALID_CREDENTIALS);
                });


        // ─── Account status guards ────────────────────────────────────
        // These are checked here (not just in AuthServiceImpl) so that
        // Spring Security's own authentication infrastructure also enforces them.

        if (pbUserEntity.getStatus() == UserStatus.LOCKED) {
            log.warn("UserDetailsService: account locked for userId={}", pbUserEntity.getEntityId());
            throw new LockedException(ResponseMessages.ACCOUNT_TEMPORARILY_LOCKED);
        }

        if (pbUserEntity.getStatus() == UserStatus.SUSPENDED) {
            log.warn("UserDetailsService: account suspended for userId={}", pbUserEntity.getEntityId());
            throw new DisabledException(ResponseMessages.ACCOUNT_SUSPENDED);
        }

        if (pbUserEntity.getStatus() == UserStatus.INACTIVE
                || pbUserEntity.getStatus() == UserStatus.PENDING_VERIFICATION) {
            log.warn("UserDetailsService: account inactive for userId={}", pbUserEntity.getEntityId());
            throw new DisabledException(ResponseMessages.ACCOUNT_NOT_ACTIVE);
        }

        log.debug("User loaded: id={} role={}", pbUserEntity.getEntityId(), pbUserEntity.getRole());
        return pbUserEntity;
    }
}
