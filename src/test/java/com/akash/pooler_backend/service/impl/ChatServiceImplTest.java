package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.ChatService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class ChatServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(ChatServiceImpl.class, ChatService.class);
    }
}
