package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbEntityIdSequence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PbEntitySequenceRepository extends JpaRepository<PbEntityIdSequence, Long> {


}
