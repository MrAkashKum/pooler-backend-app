package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * @author Akash Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
    @NotBlank(message="Refresh token is required")
    private String refreshToken;
}
