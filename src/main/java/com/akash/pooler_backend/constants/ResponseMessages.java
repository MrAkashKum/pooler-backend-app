package com.akash.pooler_backend.constants;

import com.akash.pooler_backend.enums.InvitationStatusEnums;
import com.akash.pooler_backend.enums.RideStatus;

/**
 * Centralized user-facing API and exception messages.
 *
 * Keep messages here when they can be returned to mobile clients. Logs should
 * use technical context separately and must avoid personal data.
 */
public final class ResponseMessages {

    public static final String UTILITY_CLASS = "Utility class";

    private ResponseMessages() {
        throw new IllegalStateException(UTILITY_CLASS);
    }

    public static final String RESOURCE_CREATED = "Resource created successfully";

    public static final String SIGNUP_CREATED = "Signup created. Check your email to activate your account.";
    public static final String EMAIL_VERIFIED = "Email verified. Please sign in.";
    public static final String VERIFICATION_LINK_SENT_IF_PENDING =
            "If this email is pending verification, a new activation link has been sent.";
    public static final String LOGGED_OUT = "Logged out successfully";
    public static final String LOGGED_OUT_ALL = "Logged out from all devices";
    public static final String RESET_LINK_SENT_IF_REGISTERED =
            "If this email is registered, a reset link has been sent.";
    public static final String PASSWORD_RESET_SUCCESS = "Password reset successful. Please login.";

    public static final String PROFILE_UPDATED = "Profile updated";
    public static final String PROFILE_MEDIA_UPDATED = "Profile media updated";
    public static final String PASSWORD_CHANGED_LOGIN_AGAIN = "Password changed. Please login again.";
    public static final String ACCOUNT_DELETED = "Account deleted successfully";

    public static final String CONTACT_REMOVED = "Contact removed";
    public static final String LOCATION_UPDATED = "Location updated";
    public static final String LOCATION_DELETED = "Location deleted";
    public static final String CHAT_MESSAGES_MARKED_READ = "Messages marked as read";
    public static final String CHAT_CLOSED = "Chat closed";
    public static final String FEEDBACK_DELETED = "Feedback deleted";
    public static final String SESSION_REVOKED = "Session revoked successfully";
    public static final String TELEGRAM_PROFILE_REMOVED = "Telegram profile removed";
    public static final String TELEGRAM_HANDLE_SHARED = "Telegram handle shared";

    public static final String INVITATION_ACCEPTED = "Invitation accepted";
    public static final String INVITATION_DECLINED = "Invitation declined";
    public static final String INVITATION_CANCELLED = "Invitation cancelled";
    public static final String INVITATION_PICKUP_CONFIRMED_WAITING =
            "Pickup confirmed; waiting on the other party";
    public static final String INVITATION_RIDE_CREATED = "Both parties confirmed - ride created";
    public static final String INVITATION_ALREADY_DECLINED = "Invitation already declined";
    public static final String INVITATION_ACCEPTED_REQUIRED = "Invitation must be ACCEPTED before confirming pickup";
    public static final String INVITATION_PARTICIPANT_BUSY =
            "This rider is already in an active meetup. Please try another match.";
    public static final String INVITATION_PENDING_PAIR_EXISTS =
            "You already sent this rider an invitation. Please wait for their response or try again after it expires.";

    public static final String RIDE_STATUS_UPDATED = "Ride status updated";
    public static final String RIDE_CANCELLED = "Ride cancelled";
    public static final String FARE_SPLIT_UPDATED = "Fare split updated";
    public static final String RIDE_HANDOFF_LOCKED =
            "Cab handoff and fare split unlock after both riders confirm arrival at the Common Point";
    public static final String FARE_TOTAL_REQUIRED = "Total fare must be greater than zero";
    public static final String FARE_TOTAL_TOO_HIGH = "Total fare is too high";
    public static final String FARE_PROVIDER_REQUIRED = "Cab provider is required";
    public static final String FARE_DISTANCE_UNAVAILABLE = "Ride distance is not available for fare split";

    public static final String INVALID_CREDENTIALS = "Invalid credentials";
    public static final String AUTHENTICATION_REQUIRED = "Authentication required";
    public static final String AUTH_CONTEXT_MISSING = "No authenticated user in security context";
    public static final String ACCOUNT_TEMPORARILY_LOCKED = "Account is temporarily locked";
    public static final String ACCOUNT_SUSPENDED = "Account has been suspended";
    public static final String ACCOUNT_NOT_ACTIVE = "Account is not active";
    public static final String PASSWORDS_DO_NOT_MATCH = "Passwords do not match";
    public static final String CURRENT_PASSWORD_INCORRECT = "Current password is incorrect";
    public static final String NEW_PASSWORDS_DO_NOT_MATCH = "New passwords do not match";
    public static final String NEW_PASSWORD_MUST_DIFFER = "New password must differ from current";

    public static final String GOOGLE_SIGN_IN_NOT_CONFIGURED = "Google sign-in is not configured on this server";
    public static final String GOOGLE_ID_TOKEN_INVALID = "Invalid Google ID token";
    public static final String GOOGLE_TOKEN_VALIDATION_FAILED = "Google token validation failed";
    public static final String GOOGLE_SIGN_IN_VERIFY_FAILED = "Unable to verify Google sign-in";
    public static final String APPLE_SIGN_IN_NOT_CONFIGURED = "Apple sign-in is not configured on this server";
    public static final String APPLE_ID_TOKEN_INVALID = "Invalid Apple identity token";
    public static final String APPLE_TOKEN_VALIDATION_FAILED = "Apple token validation failed";
    public static final String APPLE_SIGN_IN_VERIFY_FAILED = "Unable to verify Apple sign-in";

    public static final String JWT_TOKEN_EXPIRED = "JWT token has expired";
    public static final String JWT_TOKEN_INVALID = "JWT token is invalid";
    public static final String REFRESH_TOKEN_NOT_FOUND = "Refresh token not found";
    public static final String REFRESH_TOKEN_REVOKED = "Refresh token has been revoked";

    public static final String FILE_UPLOAD_FAILED = "File upload failed";
    public static final String FILE_EMPTY = "File is empty";
    public static final String FILE_CAPACITY_REACHED = "Temporary file capacity reached. Please try again later.";
    public static final String FILE_TYPE_NOT_ALLOWED = "Only JPEG, PNG, WebP, PDF, and text files are allowed";
    public static final String FILE_PATH_INVALID = "Invalid file path";
    public static final String FILE_STORE_FAILED = "Could not store chat file";
    public static final String PROFILE_MEDIA_REQUIRED = "Profile media file is required";
    public static final String PROFILE_MEDIA_IMAGE_ONLY = "Only JPEG, PNG, and WebP images are allowed";
    public static final String PROFILE_MEDIA_READ_FAILED = "Could not read uploaded profile media";
    public static final String PROFILE_MEDIA_GCS_UPLOAD_FAILED = "Could not upload profile media to Cloud Storage";
    public static final String PAYMENT_QR_NOT_CONFIGURED = "Add a payment QR before sharing it";
    public static final String PAYMENT_QR_NOT_SHARED = "Payment QR is not shared with you or is no longer available";
    public static final String PAYMENT_QR_SHARE_NOT_ALLOWED = "Payment QR can be shared after the cab is booked and during the journey";
    public static final String GCS_BUCKET_NOT_CONFIGURED = "Cloud Storage bucket is not configured";
    public static final String GCS_KEY_PREFIX_NOT_CONFIGURED = "Cloud Storage key prefix is not configured";

    public static final String MAIL_SEND_FAILED = "Failed to send mail";
    public static final String MESSAGE_EDIT_SENDER_ONLY = "Only the sender can edit this message";
    public static final String MESSAGE_EDIT_WINDOW_EXPIRED = "Messages can only be edited within 15 minutes of sending";
    public static final String CHAT_ARCHIVE_READ_FAILED = "Could not read chat archive";
    public static final String CHAT_ARCHIVE_WRITE_FAILED = "Could not archive chat";
    public static final String INVALID_REQUEST_BODY = "Invalid request body";
    public static final String REQUEST_METHOD_NOT_SUPPORTED = "HTTP method is not supported for this endpoint";
    public static final String UNSUPPORTED_MEDIA_TYPE = "Unsupported content type";
    public static final String MEDIA_TYPE_NOT_ACCEPTABLE = "Requested response type is not supported";

    public static String unsupportedDiscoveryMode(String value) {
        return "Unsupported discovery mode: " + value;
    }

    public static String invalidParameterType(String name) {
        return "Invalid parameter type: " + name;
    }

    public static String missingRequiredParameter(String name) {
        return "Missing required parameter: " + name;
    }

    public static String missingPathVariable(String name) {
        return "Missing path variable: " + name;
    }

    public static String locationAliasAlreadyExists(String alias) {
        return "A " + alias + " location already exists for this user";
    }

    public static String feedbackNotFound(String feedbackEntityId) {
        return "Feedback not found: " + feedbackEntityId;
    }

    public static String adminUserSuspended(String userEntityId) {
        return "User suspended: " + userEntityId;
    }

    public static String adminUserActivated(String userEntityId) {
        return "User activated: " + userEntityId;
    }

    public static String rideAlready(RideStatus status) {
        return "Ride is already " + status;
    }

    public static String liveLocationTerminalRide(RideStatus status) {
        return "Cannot publish location for a " + status + " ride";
    }

    public static String invitationStatusCannotBeModified(InvitationStatusEnums status) {
        return "Invitation is " + status + " and cannot be modified";
    }

    public static String invitationRetryLocked(int lockHours) {
        return "This rider declined your invitation. You can invite them again after " + lockHours + " hours.";
    }

    public static String accountLockedAfterAttempts(int attempts) {
        return "Account locked after " + attempts + " failed attempts";
    }

    public static String profileMediaMaxSize(long maxSizeMb) {
        return "Profile media must be %d MB or smaller".formatted(maxSizeMb);
    }

    public static String chatArchiveWriteFailed(String threadId) {
        return CHAT_ARCHIVE_WRITE_FAILED + " " + threadId;
    }
}
