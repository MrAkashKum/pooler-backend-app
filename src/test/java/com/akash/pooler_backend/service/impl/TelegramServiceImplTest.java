package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.TelegramService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class TelegramServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(TelegramServiceImpl.class, TelegramService.class);
    }
}
