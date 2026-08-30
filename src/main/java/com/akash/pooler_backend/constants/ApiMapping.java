package com.akash.pooler_backend.constants;

/**
 * Central place for backend route mappings.
 *
 * Keep values compile-time constants so Spring mapping annotations can refer
 * to them directly.
 *
 * @author Akash Kumar
 */
public final class ApiMapping {

    private ApiMapping() {
        throw new IllegalArgumentException(ResponseMessages.UTILITY_CLASS);
    }

    public static final String API_V1 = "/api/v1";

    public static final String ACTUATOR_API = "/actuator";
    public static final String ADMIN_SECURITY_API = "/admin";
    public static final String MODERATOR_SECURITY_API = "/moderator";
    public static final String H2_CONSOLE_API = "/h2-console";
    public static final String API_ROOT_MATCHER = "/api/**";

    public static final String PUBLIC_API = "/api/v1/public";
    public static final String AUTH_API = "/api/v1/auth";
    public static final String ADMIN_API = "/api/v1/admin";
    public static final String AUDIT_API = "/api/v1/audit";
    public static final String SESSIONS_API = "/api/v1/sessions";
    public static final String USERS_API = "/api/v1/users";
    public static final String CHATS_API = "/api/v1/chats";
    public static final String CHAT_FILES_API = "/api/v1/chat-files";
    public static final String CONTACTS_API = "/api/v1/contacts";
    public static final String DISCOVERY_API = "/api/v1/discovery";
    public static final String FEEDBACK_API = "/api/v1/feedback";
    public static final String GEO_API = "/api/v1/geo";
    public static final String INVITATIONS_API = "/api/v1/invitations";
    public static final String LOCATIONS_API = "/api/v1/locations";
    public static final String RIDES_API = "/api/v1/rides";
    public static final String RIDE_LIVE_LOCATION_API = "/api/v1/rides/{rideEntityId}/live-location";
    public static final String SAFETY_REPORTS_API = "/api/v1/safety-reports";
    public static final String TELEGRAM_API = "/api/v1/telegram";

    public static final String ROOT = "";
    public static final String ALL = "/**";
    public static final String ME = "/me";
    public static final String MEDIA = "/me/media";
    public static final String CHANGE_PASSWORD = "/me/change-password";

    public static final String REGISTER = "/register";
    public static final String VERIFY_EMAIL = "/verify-email";
    public static final String RESEND_VERIFICATION = "/resend-verification";
    public static final String LOGIN = "/login";
    public static final String GOOGLE = "/google";
    public static final String APPLE = "/apple";
    public static final String REFRESH = "/refresh";
    public static final String LOGOUT = "/logout";
    public static final String LOGOUT_ALL = "/logout-all";
    public static final String FORGOT_PASSWORD = "/forgot-password";
    public static final String RESET_PASSWORD = "/reset-password";

    public static final String HEALTH = "/health";
    public static final String VERSION = "/version";

    public static final String USERS = "/users";
    public static final String USER_ID = "/users/{id}";
    public static final String USER_SUSPEND = "/users/{id}/suspend";
    public static final String USER_ACTIVATE = "/users/{id}/activate";
    public static final String AUDIT_ME = "/me";
    public static final String AUDIT_USER = "/users/{entityId}";

    public static final String SESSION_ID = "/{sessionId}";
    public static final String TOKEN_INFO = "/token-info";

    public static final String BY_INVITATION = "/by-invitation/{invitationId}";
    public static final String THREAD_ID = "/{threadId}";
    public static final String THREAD_MESSAGES = "/{threadId}/messages";
    public static final String THREAD_MESSAGE = "/{threadId}/messages/{messageId}";
    public static final String THREAD_READ = "/{threadId}/read";
    public static final String THREAD_MESSAGE_REACTIONS = "/{threadId}/messages/{messageId}/reactions";
    public static final String THREAD_MESSAGE_RECEIPTS = "/{threadId}/messages/{messageId}/receipts";
    public static final String THREAD_SEARCH = "/{threadId}/search";
    public static final String THREAD_FILES = "/{threadId}/files";
    public static final String FILE_ID = "/{fileId}";

    public static final String CONTACT_FAVORITE = "/{contactEntityId}/favorite";
    public static final String CONTACT_ID = "/{contactEntityId}";

    public static final String TOGGLE = "/toggle";
    public static final String PING = "/ping";
    public static final String STATUS = "/status";
    public static final String NEARBY = "/nearby";

    public static final String DISTANCE = "/distance";
    public static final String MIDPOINT = "/midpoint";
    public static final String ROUTE_COMPATIBILITY = "/route-compatibility";

    public static final String INVITATION_ID = "/{invitationEntityId}";
    public static final String INVITATION_ACCEPT = "/{invitationEntityId}/accept";
    public static final String INVITATION_DECLINE = "/{invitationEntityId}/decline";
    public static final String INVITATION_CONFIRM_PICKUP = "/{invitationEntityId}/confirm-pickup";
    public static final String INVITATION_CANCEL = "/{invitationEntityId}/cancel";
    public static final String INBOX = "/inbox";
    public static final String OUTBOX = "/outbox";

    public static final String LOCATION_ID = "/{locationEntityId}";

    public static final String RIDE_ID = "/{rideEntityId}";
    public static final String ACTIVE = "/active";
    public static final String HISTORY = "/history";
    public static final String RIDE_STATUS = "/{rideEntityId}/status";
    public static final String RIDE_CANCEL = "/{rideEntityId}/cancel";
    public static final String FARE_SPLIT = "/{rideEntityId}/fare-split";
    public static final String ARRIVE = "/{rideEntityId}/arrive";
    public static final String PAYMENT_QR_STATUS = "/{rideEntityId}/payment-qr/status";
    public static final String PAYMENT_QR_SHARE = "/{rideEntityId}/payment-qr/share";
    public static final String PAYMENT_QR_DOWNLOAD = "/{rideEntityId}/payment-qr";

    public static final String TELEGRAM_CHAT_SHARE = "/chats/{threadId}/share";
    public static final String FEEDBACK_ID = "/{feedbackEntityId}";

    public static final String V3_API_DOCS = "/v3/api-docs";
    public static final String V3_API_DOCS_ALL = "/v3/api-docs/**";
    public static final String V3_API_DOCS_YAML = "/v3/api-docs.yaml";
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String SWAGGER_RESOURCES = "/swagger-resources/**";
    public static final String WEBJARS = "/webjars/**";
    public static final String H2_CONSOLE_ALL = "/h2-console/**";

    public static final String AUTH_REGISTER_MATCHER = "/**/auth/register";
    public static final String AUTH_VERIFY_EMAIL_MATCHER = "/**/auth/verify-email";
    public static final String AUTH_RESEND_VERIFICATION_MATCHER = "/**/auth/resend-verification";
    public static final String AUTH_LOGIN_MATCHER = "/**/auth/login";
    public static final String AUTH_GOOGLE_MATCHER = "/**/auth/google";
    public static final String AUTH_APPLE_MATCHER = "/**/auth/apple";
    public static final String AUTH_REFRESH_MATCHER = "/**/auth/refresh";
    public static final String AUTH_FORGOT_PASSWORD_MATCHER = "/**/auth/forgot-password";
    public static final String AUTH_RESET_PASSWORD_MATCHER = "/**/auth/reset-password";

    public static final String PUBLIC_HEALTH_SERVLET_PATH = "/public/health";
    public static final String PUBLIC_VERSION_SERVLET_PATH = "/public/version";
}
