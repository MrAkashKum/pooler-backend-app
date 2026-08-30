package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.ChatSearchService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class ChatSearchServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(ChatSearchServiceImpl.class, ChatSearchService.class);
    }
}
