package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.ProfileMediaService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class ProfileMediaServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(ProfileMediaServiceImpl.class, ProfileMediaService.class);
    }
}
