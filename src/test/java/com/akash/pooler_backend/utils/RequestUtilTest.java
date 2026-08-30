package com.akash.pooler_backend.utils;

import com.akash.pooler_backend.enums.PlatformType;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestUtilTest {

    @Test
    void followsUtilityClassContract() {
        ArchitectureAssertions.assertUtilityClass(RequestUtil.class);
    }

    @Test
    void extractsBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-123 ");

        assertEquals("token-123", RequestUtil.extractBearerToken(request));
    }

    @Test
    void rejectsMissingBearerPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic token-123");

        assertNull(RequestUtil.extractBearerToken(request));
    }

    @Test
    void resolvesPlatformFromExplicitHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Platform", "ios");

        assertEquals(PlatformType.IOS.name(), RequestUtil.resolvePlatform(request));
    }

    @Test
    void resolvesPlatformFromUserAgentFallback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 Android");

        assertEquals(PlatformType.ANDROID.name(), RequestUtil.resolvePlatform(request));
    }

    @Test
    void usesFirstForwardedIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");

        assertEquals("10.0.0.1", RequestUtil.getClientIp(request));
    }
}
