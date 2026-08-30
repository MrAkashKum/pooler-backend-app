package com.akash.pooler_backend.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketChatMessage {
    private String action;
    private String threadId;
    private String senderId;
    private Long timestamp;
    private Map<String, Object> payload;

    public enum Action {
        MESSAGE,
        REACTION,
        TYPING,
        READ,
        DISCONNECT
    }
}
