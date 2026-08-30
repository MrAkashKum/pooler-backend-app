package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.SavedLocationService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class SavedLocationServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(SavedLocationServiceImpl.class, SavedLocationService.class);
    }
}
