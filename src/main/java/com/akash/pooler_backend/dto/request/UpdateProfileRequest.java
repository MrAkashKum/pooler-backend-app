package com.akash.pooler_backend.dto.request;

import com.akash.pooler_backend.enums.Gender;
import com.akash.pooler_backend.enums.MatchPreference;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * @author Akash Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min=2,max=100)
    private String firstName;
    @Size(min=2,max=100)
    private String lastName;
    private Gender gender;
    private MatchPreference matchPreference;
    @Size(max=120)
    private String emergencyContactName;
    @Size(max=32)
    private String emergencyContactPhone;
    @Size(max=300)
    private String emergencyMessage;

    @AssertTrue(message = "Display name is required")
    public boolean isFirstNameValidWhenProvided() {
        return firstName == null || !firstName.trim().isBlank();
    }

    @AssertTrue(message = "Last name cannot be blank")
    public boolean isLastNameValidWhenProvided() {
        return lastName == null || !lastName.trim().isBlank();
    }

    @AssertTrue(message = "Please choose Men, Women, or Other")
    public boolean isGenderSelectedWhenProvided() {
        return gender == null || gender != Gender.UNKNOWN;
    }

    @AssertTrue(message = "Emergency contact phone is too short")
    public boolean isEmergencyPhoneReasonableWhenProvided() {
        return emergencyContactPhone == null || emergencyContactPhone.trim().isBlank()
                || emergencyContactPhone.trim().replaceAll("[^0-9+]", "").length() >= 6;
    }
}
