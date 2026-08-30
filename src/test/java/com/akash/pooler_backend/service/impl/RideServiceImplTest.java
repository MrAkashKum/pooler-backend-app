package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.RideService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class RideServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(RideServiceImpl.class, RideService.class);
    }
}
