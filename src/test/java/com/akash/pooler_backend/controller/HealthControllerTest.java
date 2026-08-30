package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class HealthControllerTest {

    @Test
    void exposesRestControllerContract() {
        ArchitectureAssertions.assertRestController(HealthController.class);
    }
}
