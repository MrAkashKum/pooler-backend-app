package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.MailService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class MailServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(MailServiceImpl.class, MailService.class);
    }
}
