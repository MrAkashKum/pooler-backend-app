package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Akash Kumar
 */
public interface PbFeedbackRepository extends JpaRepository<PbFeedbackEntity, Long> {

    List<PbFeedbackEntity> findAllByOrderByCreatedAtDesc();

    Optional<PbFeedbackEntity> findByEntityId(String entityId);

    long deleteBySubmitterEntityId(String submitterEntityId);
}
