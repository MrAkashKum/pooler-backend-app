package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbLiveLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Akash Kumar
 */
public interface PbLiveLocationRepository extends JpaRepository<PbLiveLocationEntity, Long> {

    Optional<PbLiveLocationEntity> findByRideEntityIdAndUserEntityId(String rideEntityId, String userEntityId);

    List<PbLiveLocationEntity> findAllByRideEntityId(String rideEntityId);

    long deleteByUserEntityId(String userEntityId);

    long deleteByRideEntityIdIn(List<String> rideEntityIds);
}
