package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.enums.InvitationStatusEnums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Akash Kumar
 */
public interface PbRideInvitationRepository extends JpaRepository<PbRideInvitationEntity, Long> {

    Optional<PbRideInvitationEntity> findByEntityId(String entityId);

    List<PbRideInvitationEntity> findAllByReceiverEntityIdAndStatusOrderByCreatedAtDesc(
            String receiverEntityId, InvitationStatusEnums status);

    List<PbRideInvitationEntity> findAllBySenderEntityIdOrderByCreatedAtDesc(String senderEntityId);

    List<PbRideInvitationEntity> findAllByReceiverEntityIdOrderByCreatedAtDesc(String receiverEntityId);

    @Query("""
            SELECT COUNT(i) > 0 FROM PbRideInvitationEntity i
             WHERE i.senderEntityId = :senderEntityId
               AND i.receiverEntityId = :receiverEntityId
               AND i.status = :status
               AND i.expiresAt > :now
            """)
    boolean existsPendingPair(
            @Param("senderEntityId") String senderEntityId,
            @Param("receiverEntityId") String receiverEntityId,
            @Param("status") InvitationStatusEnums status,
            @Param("now") Instant now);

    @Query("""
            SELECT COUNT(i) > 0 FROM PbRideInvitationEntity i
             WHERE i.senderEntityId = :senderEntityId
               AND i.receiverEntityId = :receiverEntityId
               AND i.respondedByEntityId = :receiverEntityId
               AND i.status = :status
               AND i.respondedAt >= :cutoff
            """)
    boolean existsRecentReceiverDecline(
            @Param("senderEntityId") String senderEntityId,
            @Param("receiverEntityId") String receiverEntityId,
            @Param("status") InvitationStatusEnums status,
            @Param("cutoff") Instant cutoff);

    @Query("""
            SELECT COUNT(i) > 0 FROM PbRideInvitationEntity i
             WHERE (i.senderEntityId = :userEntityId OR i.receiverEntityId = :userEntityId)
               AND i.status = :status
               AND (i.senderConfirmed = false OR i.receiverConfirmed = false)
            """)
    boolean existsActiveAcceptedMeetup(
            @Param("userEntityId") String userEntityId,
            @Param("status") InvitationStatusEnums status);

    @Query("""
            SELECT i.entityId FROM PbRideInvitationEntity i
             WHERE i.senderEntityId = :userEntityId OR i.receiverEntityId = :userEntityId
            """)
    List<String> findEntityIdsForUser(@Param("userEntityId") String userEntityId);

    @Modifying
    @Query("""
            DELETE FROM PbRideInvitationEntity i
             WHERE i.senderEntityId = :userEntityId OR i.receiverEntityId = :userEntityId
            """)
    int deleteAllForUser(@Param("userEntityId") String userEntityId);

    @Modifying
    @Query("""
            UPDATE PbRideInvitationEntity i
               SET i.status = :status,
                   i.respondedAt = :now,
                   i.respondedByEntityId = :systemActor
             WHERE i.status = :pendingStatus
               AND i.entityId <> :acceptedInvitationEntityId
               AND (i.senderEntityId IN :participantEntityIds OR i.receiverEntityId IN :participantEntityIds)
            """)
    int declinePendingForParticipantsExcept(
            @Param("participantEntityIds") List<String> participantEntityIds,
            @Param("acceptedInvitationEntityId") String acceptedInvitationEntityId,
            @Param("pendingStatus") InvitationStatusEnums pendingStatus,
            @Param("status") InvitationStatusEnums status,
            @Param("systemActor") String systemActor,
            @Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE PbRideInvitationEntity i
               SET i.status = 'DECLINED'
             WHERE i.status = :status
               AND i.expiresAt < :now
            """)
    int markExpired(@Param("status") InvitationStatusEnums status, @Param("now") Instant now);
}
