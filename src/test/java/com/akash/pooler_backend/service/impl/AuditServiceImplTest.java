package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.AuditService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class AuditServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(AuditServiceImpl.class, AuditService.class);
    }
}
