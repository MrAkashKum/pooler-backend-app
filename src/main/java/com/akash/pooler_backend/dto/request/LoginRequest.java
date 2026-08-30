package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest{
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String password;
    private String deviceId;
    private String platform;
    private String appVersion;


}
