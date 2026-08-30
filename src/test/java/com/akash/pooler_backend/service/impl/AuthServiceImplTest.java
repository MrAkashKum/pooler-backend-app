package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.AuthService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class AuthServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(AuthServiceImpl.class, AuthService.class);
    }
}
