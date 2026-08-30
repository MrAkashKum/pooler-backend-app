package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbTelegramProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PbTelegramProfileRepository extends JpaRepository<PbTelegramProfileEntity, Long> {

    Optional<PbTelegramProfileEntity> findByUserEntityId(String userEntityId);

    Optional<PbTelegramProfileEntity> findByTelegramHandle(String telegramHandle);

    void deleteByUserEntityId(String userEntityId);
}
