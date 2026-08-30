package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.ChangePasswordRequest;
import com.akash.pooler_backend.dto.request.UpdateProfileRequest;
import com.akash.pooler_backend.dto.response.UserResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.exception.AuthenticationException;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.*;
import com.akash.pooler_backend.service.TokenService;
import com.akash.pooler_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * @author Akash Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final PbUserRepository pbUserRepository;
    private final PbRefreshTokenRepository refreshTokenRepository;
    private final PbUserSessionRepository userSessionRepository;
    private final PbEmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PbPasswordResetTokenRepository passwordResetTokenRepository;
    private final PbDiscoveryStatusRepository discoveryStatusRepository;
    private final PbSavedLocationRepository savedLocationRepository;
    private final PbTelegramProfileRepository telegramProfileRepository;
    private final PbContactRepository contactRepository;
    private final PbSafetyReportRepository safetyReportRepository;
    private final PbRideInvitationRepository rideInvitationRepository;
    private final PbRideRepository rideRepository;
    private final PbLiveLocationRepository liveLocationRepository;
    private final PbChatThreadRepository chatThreadRepository;
    private final PbChatMessageRepository chatMessageRepository;
    private final PbMessageReactionRepository messageReactionRepository;
    private final PbChatSearchIndexRepository chatSearchIndexRepository;
    private final PbChatArchiveRepository chatArchiveRepository;
    private final PbAuditLogRepository auditLogRepository;
    private final PbFeedbackRepository feedbackRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(PbUserEntity user) {
        return UserResponse.from(user);
    }


    @Override
    @Transactional
    @AuditAction("PROFILE_UPDATE")
    public UserResponse updateProfile(PbUserEntity pbUserEntity, UpdateProfileRequest req) {
        if (req.getFirstName() != null)
            pbUserEntity.setFirstName(req.getFirstName().trim());
        if (req.getLastName() != null)
            pbUserEntity.setLastName(req.getLastName().trim());
        if (req.getGender() != null)
            pbUserEntity.setGender(req.getGender());
        if (req.getMatchPreference() != null)
            pbUserEntity.setMatchPreference(com.akash.pooler_backend.enums.MatchPreference.normalized(req.getMatchPreference()));
        if (req.getEmergencyContactName() != null)
            pbUserEntity.setEmergencyContactName(trimToNull(req.getEmergencyContactName()));
        if (req.getEmergencyContactPhone() != null)
            pbUserEntity.setEmergencyContactPhone(trimToNull(req.getEmergencyContactPhone()));
        if (req.getEmergencyMessage() != null)
            pbUserEntity.setEmergencyMessage(trimToNull(req.getEmergencyMessage()));
        return UserResponse.from(pbUserRepository.save(pbUserEntity));
    }

    private static String trimToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    @Override
    public void changePassword(PbUserEntity pbUserEntity, ChangePasswordRequest req) {
        if (!passwordEncoder.matches(req.getCurrentPassword(), pbUserEntity.getPasswordHash())) {
            throw new AuthenticationException(ResponseMessages.CURRENT_PASSWORD_INCORRECT);
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new AuthenticationException(ResponseMessages.NEW_PASSWORDS_DO_NOT_MATCH);
        }
        if (passwordEncoder.matches(req.getNewPassword(), pbUserEntity.getPasswordHash())) {
            throw new AuthenticationException(ResponseMessages.NEW_PASSWORD_MUST_DIFFER);
        }
        pbUserEntity.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        pbUserRepository.save(pbUserEntity);
        tokenService.revokeAllUserTokens(pbUserEntity); // Security: force re-login everywhere
        log.info("Password changed for userId={}", pbUserEntity.getEntityId());

    }

    @Override
    @Transactional
    public void deleteAccount(PbUserEntity pbUserEntity) {
        String userId = pbUserEntity.getEntityId();
        List<String> threadIds = nonEmpty(chatThreadRepository.findEntityIdsByParticipant(userId));
        List<String> rideIds = nonEmpty(rideRepository.findEntityIdsForUser(userId));
        List<String> messageIds = nonEmpty(chatMessageRepository.findEntityIdsForAccountDeletion(threadIds, userId));

        messageReactionRepository.deleteByUserId(userId);
        messageReactionRepository.deleteByMessageIds(messageIds);
        chatMessageRepository.deleteForAccountDeletion(threadIds, userId);
        chatSearchIndexRepository.deleteByThreadIdIn(threadIds);
        chatArchiveRepository.deleteByThreadIdIn(threadIds);
        chatThreadRepository.deleteByParticipant(userId);

        liveLocationRepository.deleteByUserEntityId(userId);
        liveLocationRepository.deleteByRideEntityIdIn(rideIds);
        rideRepository.deleteAllForUser(userId);
        rideInvitationRepository.deleteAllForUser(userId);

        safetyReportRepository.deleteByReporterEntityId(userId);
        feedbackRepository.deleteBySubmitterEntityId(userId);
        contactRepository.deleteByOwnerEntityIdOrContactUserEntityId(userId, userId);
        savedLocationRepository.deleteByUserEntityId(userId);
        telegramProfileRepository.deleteByUserEntityId(userId);
        discoveryStatusRepository.deleteByUserEntityId(userId);

        refreshTokenRepository.deleteAllByEntityId(userId);
        userSessionRepository.deleteAllByEntityId(userId);
        emailVerificationTokenRepository.deleteAllByEntityId(userId);
        passwordResetTokenRepository.deleteAllByEntityId(userId);
        auditLogRepository.deleteByEntityId(userId);

        pbUserRepository.delete(pbUserEntity);
        log.info("accountDeleted className={} methodName={} userId={}",
                getClass().getSimpleName(), "deleteAccount", userId);

    }

    private static List<String> nonEmpty(List<String> values) {
        return values == null || values.isEmpty() ? List.of("__none__") : values;
    }

    @Override
    public PbUserEntity getUserEntity(String entityId) {
        Optional<PbUserEntity> pbUserEntityOptional = pbUserRepository.findByEntityId(entityId);
        return pbUserEntityOptional.orElse(null);
    }


}
