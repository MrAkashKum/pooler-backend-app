package com.akash.pooler_backend.utils;

import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceContextUtilTest {

    @AfterEach
    void tearDown() {
        TraceContextUtil.clear();
    }

    @Test
    void followsUtilityClassContract() {
        ArchitectureAssertions.assertUtilityClass(TraceContextUtil.class);
    }

    @Test
    void usesSafeExternalCorrelationId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContextUtil.CORRELATION_ID_HEADER, "mobile-trace-0001");

        assertEquals("mobile-trace-0001", TraceContextUtil.resolveOrCreateCorrelationId(request));
    }

    @Test
    void createsCorrelationIdWhenHeaderIsUnsafe() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContextUtil.CORRELATION_ID_HEADER, "bad value with spaces");

        assertNotNull(TraceContextUtil.resolveOrCreateCorrelationId(request));
    }

    @Test
    void attachesErrorReferenceToResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String referenceId = TraceContextUtil.attachErrorReference(request, response);

        assertTrue(referenceId.startsWith("ERR-"));
        assertEquals(referenceId, response.getHeader(TraceContextUtil.ERROR_REFERENCE_ID_HEADER));
    }
}
