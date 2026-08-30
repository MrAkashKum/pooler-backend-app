package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.dto.request.SaveLocationRequest;
import com.akash.pooler_backend.dto.request.UpdateLocationRequest;
import com.akash.pooler_backend.dto.response.SavedLocationResponse;
import com.akash.pooler_backend.entity.PbSavedLocationEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.exception.LocationAliasConflictException;
import com.akash.pooler_backend.exception.LocationNotFoundException;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbSavedLocationRepository;
import com.akash.pooler_backend.service.SavedLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * @author Akash Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavedLocationServiceImpl implements SavedLocationService {

    private final PbSavedLocationRepository repository;

    @Override
    @Transactional
    @AuditAction("LOCATION_CREATE")
    public SavedLocationResponse create(PbUserEntity user, SaveLocationRequest req) {
        if (req.getAlias().isUnique()
                && repository.existsByUserEntityIdAndAlias(user.getEntityId(), req.getAlias())) {
            throw new LocationAliasConflictException(req.getAlias());
        }
        PbSavedLocationEntity entity = PbSavedLocationEntity.builder()
                .entityId(newId())
                .userEntityId(user.getEntityId())
                .alias(req.getAlias())
                .label(req.getLabel())
                .address(req.getAddress())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .build();
        entity = repository.save(entity);
        log.info("Saved location {} (alias={}) for user={}",
                entity.getEntityId(), entity.getAlias(), user.getEntityId());
        return SavedLocationResponse.from(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedLocationResponse> listForUser(PbUserEntity user) {
        return repository
                .findAllByUserEntityIdOrderByCreatedAtDesc(user.getEntityId())
                .stream()
                .map(SavedLocationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SavedLocationResponse get(PbUserEntity user, String locationEntityId) {
        return SavedLocationResponse.from(loadOwned(user, locationEntityId));
    }

    @Override
    @Transactional
    @AuditAction("LOCATION_UPDATE")
    public SavedLocationResponse update(PbUserEntity user, String locationEntityId, UpdateLocationRequest req) {
        PbSavedLocationEntity entity = loadOwned(user, locationEntityId);
        if (req.getLabel() != null)     entity.setLabel(req.getLabel());
        if (req.getAddress() != null)   entity.setAddress(req.getAddress());
        if (req.getLatitude() != null)  entity.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) entity.setLongitude(req.getLongitude());
        return SavedLocationResponse.from(repository.save(entity));
    }

    @Override
    @Transactional
    @AuditAction("LOCATION_DELETE")
    public void delete(PbUserEntity user, String locationEntityId) {
        PbSavedLocationEntity entity = loadOwned(user, locationEntityId);
        repository.delete(entity);
        log.info("Deleted location {} for user={}", locationEntityId, user.getEntityId());
    }

    // ─── helpers ──────────────────────────────────────────────────────

    private PbSavedLocationEntity loadOwned(PbUserEntity user, String locationEntityId) {
        return repository.findByEntityIdAndUserEntityId(locationEntityId, user.getEntityId())
                .orElseThrow(LocationNotFoundException::new);
    }

    private static String newId() {
        return "loc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
