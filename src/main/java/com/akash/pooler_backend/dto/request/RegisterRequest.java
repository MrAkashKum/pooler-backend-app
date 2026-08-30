package com.akash.pooler_backend.dto.request;

import com.akash.pooler_backend.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * @author Akash Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message="First name is required")
    @Size(min=2,max=100,message="First name must be 2-100 characters")
    private String firstName;

    @NotBlank(message="Last name is required")
    @Size(min=2,max=100, message="Last name must be 2-100 characters")
    private String lastName;

    @NotBlank(message="Email is required")
    @Email(message="Must be a valid email address")
    private String email;

    @NotBlank(message="Password is required")
    @Size(min=8,max=72, message="Password must be 8-72 characters")
    @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
            message="Password must contain uppercase, lowercase, digit and special character")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotNull(message = "Gender is required")
    private Gender gender;
    /**
     * Mobile device metadata
      */
    private String deviceId;
    /**
     * ANDROID | IOS
     *  */
    private String platform;
    private String appVersion;
    /**
     * Firebase push notification token
     */
    private String fcmToken;
}
