package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbRideEntity;
import com.akash.pooler_backend.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * @author Akash Kumar
 */
public interface PbRideRepository extends JpaRepository<PbRideEntity, Long> {

    Optional<PbRideEntity> findByEntityId(String entityId);

    @Query("""
            SELECT r FROM PbRideEntity r
             WHERE (r.primaryEntityId = :userEntityId OR r.secondaryEntityId = :userEntityId)
             ORDER BY r.createdAt DESC
            """)
    List<PbRideEntity> findHistoryForUser(@Param("userEntityId") String userEntityId);

    @Query("""
            SELECT r FROM PbRideEntity r
             WHERE (r.primaryEntityId = :userEntityId OR r.secondaryEntityId = :userEntityId)
               AND r.status NOT IN :terminalStatuses
             ORDER BY r.createdAt DESC
            """)
    List<PbRideEntity> findActiveForUser(
            @Param("userEntityId") String userEntityId,
            @Param("terminalStatuses") List<RideStatus> terminalStatuses);

    @Query("""
            SELECT COUNT(r) > 0 FROM PbRideEntity r
             WHERE (r.primaryEntityId = :userEntityId OR r.secondaryEntityId = :userEntityId)
               AND r.status NOT IN :terminalStatuses
            """)
    boolean existsActiveForUser(
            @Param("userEntityId") String userEntityId,
            @Param("terminalStatuses") List<RideStatus> terminalStatuses);

    @Query("""
            SELECT r.entityId FROM PbRideEntity r
             WHERE r.primaryEntityId = :userEntityId OR r.secondaryEntityId = :userEntityId
            """)
    List<String> findEntityIdsForUser(@Param("userEntityId") String userEntityId);

    @Modifying
    @Query("""
            DELETE FROM PbRideEntity r
             WHERE r.primaryEntityId = :userEntityId OR r.secondaryEntityId = :userEntityId
            """)
    int deleteAllForUser(@Param("userEntityId") String userEntityId);
}
