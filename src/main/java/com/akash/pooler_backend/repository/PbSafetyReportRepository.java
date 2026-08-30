package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbSafetyReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Akash Kumar
 */
public interface PbSafetyReportRepository extends JpaRepository<PbSafetyReportEntity, Long> {

    List<PbSafetyReportEntity> findAllByReporterEntityIdOrderByCreatedAtDesc(String reporterEntityId);

    long deleteByReporterEntityId(String reporterEntityId);
}
