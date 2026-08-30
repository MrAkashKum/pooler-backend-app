package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.ChatArchivalService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class ChatArchivalServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(ChatArchivalServiceImpl.class, ChatArchivalService.class);
    }
}
