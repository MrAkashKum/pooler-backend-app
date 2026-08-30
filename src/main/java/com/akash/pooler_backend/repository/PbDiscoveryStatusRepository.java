package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbDiscoveryStatusEntity;
import com.akash.pooler_backend.enums.DiscoveryMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Akash Kumar
 */
public interface PbDiscoveryStatusRepository extends JpaRepository<PbDiscoveryStatusEntity, Long> {

    Optional<PbDiscoveryStatusEntity> findByUserEntityId(String userEntityId);

    /**
     * Approximate "nearby" lookup using a simple bounding-box filter on
     * latitude and longitude. The fine-grained distance check is performed
     * in the service layer using the Haversine formula. This keeps the
     * query DB-portable (no PostGIS dependency).
     */
    @Query("""
            SELECT d FROM PbDiscoveryStatusEntity d
             WHERE d.mode = :mode
               AND d.userEntityId <> :excludeEntityId
               AND d.currentLatitude  BETWEEN :minLat AND :maxLat
               AND d.currentLongitude BETWEEN :minLng AND :maxLng
               AND (d.lastPingedAt IS NULL OR d.lastPingedAt >= :freshnessAfter)
            """)
    List<PbDiscoveryStatusEntity> findNearbyCandidates(
            @Param("mode") DiscoveryMode mode,
            @Param("excludeEntityId") String excludeEntityId,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("freshnessAfter") Instant freshnessAfter
    );

    void deleteByUserEntityId(String userEntityId);
}
