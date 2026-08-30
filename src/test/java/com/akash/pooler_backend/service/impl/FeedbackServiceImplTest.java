package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.FeedbackService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class FeedbackServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(FeedbackServiceImpl.class, FeedbackService.class);
    }
}
