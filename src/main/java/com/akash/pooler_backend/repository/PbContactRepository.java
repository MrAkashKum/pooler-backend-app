package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Akash Kumar
 */
public interface PbContactRepository extends JpaRepository<PbContactEntity, Long> {

    Optional<PbContactEntity> findByEntityId(String entityId);

    Optional<PbContactEntity> findByEntityIdAndOwnerEntityId(String entityId, String ownerEntityId);

    List<PbContactEntity> findAllByOwnerEntityIdOrderByFavoriteDescCreatedAtDesc(String ownerEntityId);

    boolean existsByOwnerEntityIdAndContactUserEntityId(String ownerEntityId, String contactUserEntityId);

    long deleteByOwnerEntityIdOrContactUserEntityId(String ownerEntityId, String contactUserEntityId);
}
