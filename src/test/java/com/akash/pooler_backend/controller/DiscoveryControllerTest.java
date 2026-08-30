package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class DiscoveryControllerTest {

    @Test
    void exposesRestControllerContract() {
        ArchitectureAssertions.assertRestController(DiscoveryController.class);
    }
}
