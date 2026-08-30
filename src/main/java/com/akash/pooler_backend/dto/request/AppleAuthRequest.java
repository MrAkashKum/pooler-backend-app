package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppleAuthRequest {

    @NotBlank(message = "Apple identity token is required")
    @Size(max = 4096, message = "Apple identity token is too large")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$",
            message = "Apple identity token format is invalid")
    private String identityToken;

    @Size(max = 80, message = "First name must be at most 80 characters")
    private String firstName;

    @Size(max = 80, message = "Last name must be at most 80 characters")
    private String lastName;
}
