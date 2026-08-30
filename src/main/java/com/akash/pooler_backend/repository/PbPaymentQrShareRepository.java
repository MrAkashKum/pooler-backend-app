package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbPaymentQrShareEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PbPaymentQrShareRepository extends JpaRepository<PbPaymentQrShareEntity, Long> {

    Optional<PbPaymentQrShareEntity> findFirstByRideEntityIdAndOwnerEntityIdAndRecipientEntityIdAndRevokedAtIsNullOrderBySharedAtDesc(
            String rideEntityId, String ownerEntityId, String recipientEntityId);

    List<PbPaymentQrShareEntity> findByRideEntityIdAndOwnerEntityIdAndRecipientEntityIdAndRevokedAtIsNull(
            String rideEntityId, String ownerEntityId, String recipientEntityId);
}
