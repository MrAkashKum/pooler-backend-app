package com.akash.pooler_backend;

import com.akash.pooler_backend.dto.request.DiscoveryToggleRequest;
import com.akash.pooler_backend.dto.request.CreateSafetyReportRequest;
import com.akash.pooler_backend.dto.request.UpdateFareSplitRequest;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.entity.PbContactEntity;
import com.akash.pooler_backend.entity.PbDiscoveryStatusEntity;
import com.akash.pooler_backend.entity.PbFeedbackEntity;
import com.akash.pooler_backend.entity.PbRideEntity;
import com.akash.pooler_backend.entity.PbSafetyReportEntity;
import com.akash.pooler_backend.entity.PbSavedLocationEntity;
import com.akash.pooler_backend.enums.Gender;
import com.akash.pooler_backend.enums.LocationAlias;
import com.akash.pooler_backend.enums.Role;
import com.akash.pooler_backend.enums.RideStatus;
import com.akash.pooler_backend.enums.SafetyReportStatus;
import com.akash.pooler_backend.enums.UserStatus;
import com.akash.pooler_backend.exception.RideInvalidStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import com.akash.pooler_backend.dto.request.SendMessageRequest;
import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.enums.ChatThreadStatus;
import com.akash.pooler_backend.enums.DiscoveryMode;
import com.akash.pooler_backend.enums.InvitationStatusEnums;
import com.akash.pooler_backend.repository.PbChatArchiveRepository;
import com.akash.pooler_backend.repository.PbChatThreadRepository;
import com.akash.pooler_backend.repository.PbContactRepository;
import com.akash.pooler_backend.repository.PbDiscoveryStatusRepository;
import com.akash.pooler_backend.repository.PbEmailVerificationTokenRepository;
import com.akash.pooler_backend.repository.PbFeedbackRepository;
import com.akash.pooler_backend.repository.PbRideInvitationRepository;
import com.akash.pooler_backend.repository.PbRideRepository;
import com.akash.pooler_backend.repository.PbSafetyReportRepository;
import com.akash.pooler_backend.repository.PbSavedLocationRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.ChatService;
import com.akash.pooler_backend.service.RideService;
import com.akash.pooler_backend.service.SafetyReportService;
import com.akash.pooler_backend.service.UserService;
import com.akash.pooler_backend.security.JwtUtil;
import com.akash.pooler_backend.utils.RequestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSmokeIntegrationTests {

    private static final String TEST_PASSWORD = "Test@1234!";
    private static final String RIDER_A_EMAIL = "rider-a@hoppo.test";
    private static final String RIDER_B_EMAIL = "rider-b@hoppo.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired private PbUserRepository userRepository;
    @Autowired private PbRideInvitationRepository invitationRepository;
    @Autowired private PbRideRepository rideRepository;
    @Autowired private PbChatThreadRepository threadRepository;
    @Autowired private PbChatArchiveRepository archiveRepository;
    @Autowired private PbEmailVerificationTokenRepository emailVerificationTokenRepository;
    @Autowired private PbFeedbackRepository feedbackRepository;
    @Autowired private PbDiscoveryStatusRepository discoveryStatusRepository;
    @Autowired private PbSavedLocationRepository savedLocationRepository;
    @Autowired private PbSafetyReportRepository safetyReportRepository;
    @Autowired private PbContactRepository contactRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ChatService chatService;
    @Autowired private RideService rideService;
    @Autowired private SafetyReportService safetyReportService;
    @Autowired private UserService userService;
    @Autowired private JwtUtil jwtUtil;

    @BeforeEach
    void ensureTestUsers() {
        ensureUser("test-user-a", RIDER_A_EMAIL, "Rider", "A", Gender.MALE);
        ensureUser("test-user-b", RIDER_B_EMAIL, "Rider", "B", Gender.FEMALE);
    }

    @Test
    void publicHealthUsesApiEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void actuatorMonitoringIsAdminOnly() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());

        var rider = userRepository.findByEmail(RIDER_A_EMAIL).orElseThrow();
        mockMvc.perform(get("/actuator/info")
                        .header("Authorization", "Bearer " + jwtUtil.generateAccessToken(rider)))
                .andExpect(status().isForbidden());

        var admin = ensureUser("test-admin", "admin@hoppo.test", "Admin", "Ops", Gender.OTHER, Role.ROLE_ADMIN);
        mockMvc.perform(get("/actuator/info")
                        .header("Authorization", "Bearer " + jwtUtil.generateAccessToken(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void expoWebPreflightCanReachLoginEndpoint() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers",
                                "content-type,x-platform,x-app-version,x-device-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }

    @Test
    void discoveryToggleAcceptsLegacyVisibleMode() throws Exception {
        DiscoveryToggleRequest request = objectMapper.readValue("""
                {
                  "mode": "VISIBLE",
                  "currentLatitude": 1.3152,
                  "currentLongitude": 103.7651
                }
                """, DiscoveryToggleRequest.class);

        assertEquals(DiscoveryMode.ON, request.getMode());
    }

    @Test
    void mobileLoginReturnsAllThreeTokenTypes() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "integration-test-device")
                        .header("X-Platform", "ANDROID")
                        .header("X-App-Version", "1.0.0")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "platform": "ANDROID"
                                }
                                """.formatted(RIDER_A_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.sessionToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(RIDER_A_EMAIL));
    }

    @Test
    void errorResponsesIncludeTraceAndReferenceIds() throws Exception {
        String traceId = "mobile-trace-0001";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-ID", traceId)
                        .header("X-Device-Id", "integration-test-device")
                        .header("X-Platform", "ANDROID")
                        .header("X-App-Version", "1.0.0")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Wrong@1234",
                                  "platform": "ANDROID"
                                }
                                """.formatted(RIDER_A_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-ID", traceId))
                .andExpect(header().exists("X-Error-Reference-ID"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH-001"))
                .andExpect(jsonPath("$.traceId").value(traceId))
                .andExpect(jsonPath("$.errorReferenceId").isNotEmpty());
    }

    @Test
    void platformIsResolvedFromHeaderOrUserAgentFallback() {
        MockHttpServletRequest androidHeader = new MockHttpServletRequest();
        androidHeader.addHeader("X-Platform", "android");
        assertEquals("ANDROID", RequestUtil.getPlatform(androidHeader));

        MockHttpServletRequest iosUserAgent = new MockHttpServletRequest();
        iosUserAgent.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)");
        assertEquals("IOS", RequestUtil.getPlatform(iosUserAgent));

        MockHttpServletRequest webFallback = new MockHttpServletRequest();
        webFallback.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X)");
        assertEquals("WEB", RequestUtil.getPlatform(webFallback));
    }

    @Test
    void emailSignupRequiresVerificationBeforeLogin() throws Exception {
        String email = "signup-" + UUID.randomUUID().toString().substring(0, 8) + "@pooler.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "integration-test-device")
                        .header("X-Platform", "ANDROID")
                        .header("X-App-Version", "1.0.0")
                        .content("""
                                {
                                  "firstName": "New",
                                  "lastName": "Rider",
                                  "email": "%s",
                                  "gender": "OTHER",
                                  "password": "%s",
                                  "confirmPassword": "%s",
                                  "platform": "ANDROID"
                                }
                                """.formatted(email, TEST_PASSWORD, TEST_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "integration-test-device")
                        .header("X-Platform", "ANDROID")
                        .header("X-App-Version", "1.0.0")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "platform": "ANDROID"
                                }
                                """.formatted(email, TEST_PASSWORD)))
                .andExpect(status().isUnauthorized());

        var user = userRepository.findByEmail(email).orElseThrow();
        var token = emailVerificationTokenRepository.findAll().stream()
                .filter(candidate -> candidate.getEntityId().equals(user.getEntityId()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "token": "%s" }
                                """.formatted(token.getToken())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "integration-test-device")
                        .header("X-Platform", "ANDROID")
                        .header("X-App-Version", "1.0.0")
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "platform": "ANDROID"
                                }
                                """.formatted(email, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void acceptedInvitationChatCanSendAndArchiveMessages() {
        var akash = userRepository.findByEmail(RIDER_A_EMAIL).orElseThrow();
        var alice = userRepository.findByEmail(RIDER_B_EMAIL).orElseThrow();
        String invitationId = "test-inv-" + UUID.randomUUID().toString().substring(0, 8);
        var invitation = invitationRepository.save(PbRideInvitationEntity.builder()
                .entityId(invitationId)
                .senderEntityId(akash.getEntityId())
                .receiverEntityId(alice.getEntityId())
                .senderLat(1.3152).senderLng(103.7651)
                .senderDestLat(1.2834).senderDestLng(103.8607)
                .receiverLat(1.3008).receiverLng(103.8565)
                .receiverDestLat(1.2834).receiverDestLng(103.8607)
                .status(InvitationStatusEnums.ACCEPTED)
                .expiresAt(Instant.now().plusSeconds(300))
                .build());

        var thread = chatService.createChatThread(akash, invitation);
        chatService.sendMessage(akash, thread.getEntityId(), SendMessageRequest.builder()
                .content("I am near the blue sign")
                .messageType("TEXT")
                .build());

        var messages = chatService.getMessages(thread.getEntityId(), 0, alice, PageRequest.of(0, 10));
        org.junit.jupiter.api.Assertions.assertEquals(1, messages.getTotalElements());

        thread.setExpiresAt(Instant.now().minusSeconds(1));
        threadRepository.save(thread);
        chatService.archiveExpiredChats();
        org.junit.jupiter.api.Assertions.assertEquals(ChatThreadStatus.ARCHIVED,
                threadRepository.findByEntityId(thread.getEntityId()).orElseThrow().getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(archiveRepository.findByThreadId(thread.getEntityId()).isPresent());
    }

    @Test
    void fareSplitUsesDistanceWeightedShares() {
        var akash = userRepository.findByEmail(RIDER_A_EMAIL).orElseThrow();
        var alice = userRepository.findByEmail(RIDER_B_EMAIL).orElseThrow();
        String rideId = "ride-test-" + UUID.randomUUID().toString().substring(0, 8);
        var ride = rideRepository.save(PbRideEntity.builder()
                .entityId(rideId)
                .primaryEntityId(alice.getEntityId())
                .secondaryEntityId(akash.getEntityId())
                .pickupLat(1.3008).pickupLng(103.8565)
                .pickupAddress("Bugis MRT Exit B")
                .firstDropLat(1.3152).firstDropLng(103.7651)
                .firstDropAddress("Clementi")
                .finalDropLat(1.2834).finalDropLng(103.8607)
                .finalDropAddress("Marina Bay Sands")
                .primaryTripDistanceKm(15.0)
                .secondaryTripDistanceKm(10.0)
                .totalDistanceKm(15.0)
                .estimatedFare(30.0)
                .build());

        assertThrows(RideInvalidStateException.class, () -> rideService.updateFareSplit(akash, rideId,
                new UpdateFareSplitRequest(30.0, "SGD", "Grab")));

        ride.setPrimaryArrived(true);
        ride.setSecondaryArrived(true);
        ride.setStatus(RideStatus.AT_PICKUP);
        rideRepository.save(ride);

        var response = rideService.updateFareSplit(akash, rideId,
                new UpdateFareSplitRequest(30.0, "SGD", "Grab"));

        assertEquals(18.0, response.getPrimaryFareShare());
        assertEquals(12.0, response.getSecondaryFareShare());
        assertEquals("Grab", response.getFareSplitProvider());
    }

    @Test
    void safetyReportCreateAndReadForReporter() {
        var akash = userRepository.findByEmail(RIDER_A_EMAIL).orElseThrow();

        var created = safetyReportService.create(akash, CreateSafetyReportRequest.builder()
                .rideEntityId("ride-safety-test")
                .category("Unsafe meetup place")
                .details("The pickup point was too isolated at night.")
                .contactAllowed(true)
                .latitude(1.3008)
                .longitude(103.8565)
                .build());

        var reports = safetyReportService.listForReporter(akash);

        org.junit.jupiter.api.Assertions.assertTrue(reports.stream()
                .anyMatch(report -> report.getEntityId().equals(created.getEntityId())));
        assertEquals("OPEN", created.getStatus().name());
    }

    @Test
    void feedbackCreateIsPrivateAndAdminCanReviewAndDelete() throws Exception {
        var rider = userRepository.findByEmail(RIDER_A_EMAIL).orElseThrow();
        var admin = ensureUser("feedback-admin", "feedback-admin@hoppo.test", "Admin", "Feedback", Gender.OTHER, Role.ROLE_ADMIN);
        String riderToken = jwtUtil.generateAccessToken(rider);
        String adminToken = jwtUtil.generateAccessToken(admin);

        String response = mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + riderToken)
                        .header("X-Platform", "IOS")
                        .header("X-App-Version", "1.0.1")
                        .content("""
                                {
                                  "emotion": "SUPERB",
                                  "subject": "Matching",
                                  "rating": 5,
                                  "message": "Easy flow and friendly design."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.emotion").value("SUPERB"))
                .andExpect(jsonPath("$.data.platform").value("IOS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String feedbackEntityId = objectMapper.readTree(response).path("data").path("entityId").asText();

        mockMvc.perform(get("/api/v1/feedback")
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/feedback")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.entityId == '%s')]".formatted(feedbackEntityId)).exists());

        mockMvc.perform(delete("/api/v1/feedback/" + feedbackEntityId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertFalse(feedbackRepository.findByEntityId(feedbackEntityId).isPresent());
    }

    @Test
    void accountDeletionRemovesUserOwnedData() {
        String userId = "delete-user-" + UUID.randomUUID().toString().substring(0, 8);
        var user = userRepository.save(PbUserEntity.builder()
                .entityId(userId)
                .username(userId)
                .email(userId + "@hoppo.test")
                .firstName("Delete")
                .lastName("Me")
                .gender(Gender.OTHER)
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .build());

        discoveryStatusRepository.save(PbDiscoveryStatusEntity.builder()
                .entityId("dsc-" + UUID.randomUUID().toString().substring(0, 8))
                .userEntityId(userId)
                .mode(DiscoveryMode.ON)
                .currentLatitude(1.3008)
                .currentLongitude(103.8565)
                .build());
        savedLocationRepository.save(PbSavedLocationEntity.builder()
                .entityId("loc-" + UUID.randomUUID().toString().substring(0, 8))
                .userEntityId(userId)
                .alias(LocationAlias.HOME)
                .address("Home")
                .latitude(1.3008)
                .longitude(103.8565)
                .build());
        safetyReportRepository.save(PbSafetyReportEntity.builder()
                .entityId("safe-" + UUID.randomUUID().toString().substring(0, 8))
                .reporterEntityId(userId)
                .category("Other safety concern")
                .details("Delete verification")
                .contactAllowed(false)
                .status(SafetyReportStatus.OPEN)
                .build());
        feedbackRepository.save(PbFeedbackEntity.builder()
                .entityId("fb-" + UUID.randomUUID().toString().substring(0, 8))
                .submitterEntityId(userId)
                .emotion("SUGGESTION")
                .subject("Profile")
                .rating(4)
                .message("Delete verification")
                .platform("ANDROID")
                .appVersion("1.0.1")
                .build());
        contactRepository.save(PbContactEntity.builder()
                .entityId("con-" + UUID.randomUUID().toString().substring(0, 8))
                .ownerEntityId(userId)
                .contactUserEntityId("test-user-a")
                .build());

        userService.deleteAccount(user);

        assertFalse(userRepository.findByEntityId(userId).isPresent());
        assertFalse(discoveryStatusRepository.findByUserEntityId(userId).isPresent());
        assertEquals(0, savedLocationRepository.findAllByUserEntityIdOrderByCreatedAtDesc(userId).size());
        assertEquals(0, safetyReportRepository.findAllByReporterEntityIdOrderByCreatedAtDesc(userId).size());
        assertEquals(0, feedbackRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(feedback -> userId.equals(feedback.getSubmitterEntityId()))
                .count());
        assertEquals(0, contactRepository.findAllByOwnerEntityIdOrderByFavoriteDescCreatedAtDesc(userId).size());
    }

    private PbUserEntity ensureUser(String entityId, String email, String firstName, String lastName, Gender gender) {
        return ensureUser(entityId, email, firstName, lastName, gender, Role.ROLE_USER);
    }

    private PbUserEntity ensureUser(String entityId, String email, String firstName, String lastName, Gender gender,
                                    Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> userRepository.save(PbUserEntity.builder()
                .entityId(entityId)
                .username(entityId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .gender(gender)
                .role(role)
                .status(UserStatus.ACTIVE)
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .build()));
    }
}
