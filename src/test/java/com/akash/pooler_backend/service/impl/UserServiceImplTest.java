package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.UserService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class UserServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(UserServiceImpl.class, UserService.class);
    }
}
