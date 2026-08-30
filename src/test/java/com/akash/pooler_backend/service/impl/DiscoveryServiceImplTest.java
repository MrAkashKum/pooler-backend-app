package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.DiscoveryService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class DiscoveryServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(DiscoveryServiceImpl.class, DiscoveryService.class);
    }
}
