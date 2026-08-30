package com.akash.pooler_backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Authentication & Authorisation
    INVALID_CREDENTIALS("AUTH-001", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH-002", "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("AUTH-003", "Token is invalid or malformed", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED("AUTH-004", "Token has been revoked", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND("AUTH-005", "Token not found", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_EXPIRED("AUTH-006", "Refresh token has expired", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED("AUTH-007", "Session has expired", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH-008", "Access denied - insufficient permissions", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("AUTH-009", "Account is locked", HttpStatus.FORBIDDEN),
    ACCOUNT_SUSPENDED("AUTH-010", "Account is suspended", HttpStatus.FORBIDDEN),
    ACCOUNT_INACTIVE("AUTH-011", "Account is not active", HttpStatus.FORBIDDEN),
    MISSING_AUTH_HEADER("AUTH-012","Authorization header is missing",HttpStatus.UNAUTHORIZED),
    EMAIL_VERIFICATION_INVALID("AUTH-013", "Email verification token is invalid or expired", HttpStatus.BAD_REQUEST),

    // User
    USER_NOT_FOUND("USR-001", "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USR-002", "User with this email already exists", HttpStatus.CONFLICT),
    USER_NOT_VERIFIED("USR-003", "User email is not verified", HttpStatus.FORBIDDEN),

    // Password
    INVALID_RESET_TOKEN("PWD-001", "Password reset token is invalid or expired", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH("PWD-002", "Passwords do not match", HttpStatus.BAD_REQUEST),
    SAME_PASSWORD("PWD-003", "New password must be different from current password", HttpStatus.BAD_REQUEST),

    // Validation
    VALIDATION_ERROR("VAL-001", "Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("VAL-002", "Invalid request body", HttpStatus.BAD_REQUEST),
    REQUEST_METHOD_NOT_SUPPORTED("VAL-003", "HTTP method is not supported for this endpoint", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE("VAL-004", "Unsupported content type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    MEDIA_TYPE_NOT_ACCEPTABLE("VAL-005", "Requested response type is not supported", HttpStatus.NOT_ACCEPTABLE),

    // Rate Limiting
    RATE_LIMIT_EXCEEDED("RATE-001", "Too many requests. Please try again later.", HttpStatus.TOO_MANY_REQUESTS),

    // Mail
    MAIL_SEND_FAILED("MAIL-001", "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),

    // Saved Location
    LOCATION_NOT_FOUND("LOC-001", "Saved location not found", HttpStatus.NOT_FOUND),
    LOCATION_ALIAS_CONFLICT("LOC-002", "A location with this unique alias already exists", HttpStatus.CONFLICT),
    INVALID_COORDINATES("LOC-003", "Latitude or longitude is out of range", HttpStatus.BAD_REQUEST),

    // Contacts
    CONTACT_NOT_FOUND("CON-001", "Contact not found", HttpStatus.NOT_FOUND),
    CONTACT_ALREADY_EXISTS("CON-002", "Contact is already in your list", HttpStatus.CONFLICT),
    CONTACT_SELF_NOT_ALLOWED("CON-003", "You cannot add yourself as a contact", HttpStatus.BAD_REQUEST),

    // Discovery
    DISCOVERY_NOT_ENABLED("DSC-001", "Discovery mode is not enabled for this user", HttpStatus.BAD_REQUEST),
    DISCOVERY_LOCATION_REQUIRED("DSC-002", "Current location is required to enable discovery mode", HttpStatus.BAD_REQUEST),

    // Invitation
    INVITATION_NOT_FOUND("INV-001", "Ride invitation not found", HttpStatus.NOT_FOUND),
    INVITATION_EXPIRED("INV-002", "Ride invitation has expired", HttpStatus.GONE),
    INVITATION_ALREADY_RESOLVED("INV-003", "Ride invitation has already been resolved", HttpStatus.CONFLICT),
    INVITATION_FORBIDDEN("INV-004", "You are not allowed to act on this invitation", HttpStatus.FORBIDDEN),
    INVITATION_SELF_NOT_ALLOWED("INV-005", "You cannot send a ride invitation to yourself", HttpStatus.BAD_REQUEST),
    INVITATION_PARTICIPANT_BUSY("INV-006", "A rider is already in an active meetup", HttpStatus.CONFLICT),
    INVITATION_RETRY_LOCKED("INV-007", "Invitation retry is temporarily locked", HttpStatus.CONFLICT),

    // Ride
    RIDE_NOT_FOUND("RIDE-001", "Ride not found", HttpStatus.NOT_FOUND),
    RIDE_FORBIDDEN("RIDE-002", "You are not a participant of this ride", HttpStatus.FORBIDDEN),
    RIDE_INVALID_STATE("RIDE-003", "Ride is not in a valid state for this operation", HttpStatus.CONFLICT),
    INCOMPATIBLE_ROUTE("RIDE-004", "Routes are not compatible for ride sharing", HttpStatus.UNPROCESSABLE_ENTITY),
    PAYMENT_QR_NOT_CONFIGURED("RIDE-005", "Add a payment QR before sharing it", HttpStatus.CONFLICT),
    PAYMENT_QR_NOT_SHARED("RIDE-006", "Payment QR is not shared with you or is no longer available", HttpStatus.FORBIDDEN),

    // Chat & Messaging
    CHAT_NOT_FOUND("CHAT-001", "Chat thread not found", HttpStatus.NOT_FOUND),
    CHAT_EXPIRED("CHAT-002", "Chat session has expired (2-hour window closed)", HttpStatus.GONE),
    CHAT_ACCESS_DENIED("CHAT-003", "You do not have access to this chat", HttpStatus.FORBIDDEN),
    CHAT_INVITATION_NOT_ACCEPTED("CHAT-004", "Chat can only be created when both users accept the invitation", HttpStatus.CONFLICT),
    MESSAGE_EDIT_LIMIT_EXCEEDED("CHAT-005", "Messages can only be edited within 15 minutes of sending", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_TOO_LARGE("FILE-001", "File size exceeds the 10MB limit", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_UPLOAD_EXPIRED("FILE-002", "File upload has expired", HttpStatus.GONE),
    INVALID_REACTION("CHAT-006", "Invalid emoji reaction", HttpStatus.BAD_REQUEST),
    TELEGRAM_PROFILE_NOT_FOUND("TLG-001", "Telegram profile not found", HttpStatus.NOT_FOUND),
    WEBSOCKET_AUTH_FAILED("WS-001", "WebSocket authentication failed", HttpStatus.UNAUTHORIZED),
    ARCHIVE_NOT_FOUND("ARC-001", "Archived chat not found", HttpStatus.NOT_FOUND),

    // Generic
    INTERNAL_ERROR("SYS-001", "An internal server error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    RESOURCE_NOT_FOUND("SYS-002", "Requested resource not found", HttpStatus.NOT_FOUND);


    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;
}
