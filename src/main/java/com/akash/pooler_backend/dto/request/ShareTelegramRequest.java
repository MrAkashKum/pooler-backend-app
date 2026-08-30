package com.akash.pooler_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareTelegramRequest {
    @Pattern(regexp = "^@?[A-Za-z0-9_]{5,32}$", message = "Invalid Telegram handle")
    private String telegramHandle;
    private String telegramPhoneNumber;
}
