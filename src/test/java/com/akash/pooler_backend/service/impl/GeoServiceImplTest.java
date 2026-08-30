package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.GeoService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class GeoServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(GeoServiceImpl.class, GeoService.class);
    }
}
