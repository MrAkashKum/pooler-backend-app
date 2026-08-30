package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.SafetyReportService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class SafetyReportServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(SafetyReportServiceImpl.class, SafetyReportService.class);
    }
}
