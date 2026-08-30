package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.LiveLocationService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class LiveLocationServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(LiveLocationServiceImpl.class, LiveLocationService.class);
    }
}
