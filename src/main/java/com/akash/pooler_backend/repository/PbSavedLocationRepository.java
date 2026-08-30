package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbSavedLocationEntity;
import com.akash.pooler_backend.enums.LocationAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Akash Kumar
 */
public interface PbSavedLocationRepository extends JpaRepository<PbSavedLocationEntity, Long> {

    Optional<PbSavedLocationEntity> findByEntityId(String entityId);

    Optional<PbSavedLocationEntity> findByEntityIdAndUserEntityId(String entityId, String userEntityId);

    List<PbSavedLocationEntity> findAllByUserEntityIdOrderByCreatedAtDesc(String userEntityId);

    Optional<PbSavedLocationEntity> findByUserEntityIdAndAlias(String userEntityId, LocationAlias alias);

    boolean existsByUserEntityIdAndAlias(String userEntityId, LocationAlias alias);

    long deleteByEntityIdAndUserEntityId(String entityId, String userEntityId);

    long deleteByUserEntityId(String userEntityId);
}
