package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.TokenService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class TokenServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(TokenServiceImpl.class, TokenService.class);
    }
}
