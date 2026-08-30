package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class SafetyReportControllerTest {

    @Test
    void exposesRestControllerContract() {
        ArchitectureAssertions.assertRestController(SafetyReportController.class);
    }
}
