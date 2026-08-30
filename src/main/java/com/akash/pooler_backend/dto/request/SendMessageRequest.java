package com.akash.pooler_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {
    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message must be at most 1000 characters")
    private String content;
    private String messageType;
    private Map<String, Object> metadata;
}
