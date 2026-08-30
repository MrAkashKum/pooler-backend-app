package com.akash.pooler_backend.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketServerMessage {
    private String type;
    private String threadId;
    private String sender;
    private String senderName;
    private Long timestamp;
    private Object payload;

    public enum Type {
        MESSAGE_SENT,
        MESSAGE_EDITED,
        REACTION_ADDED,
        REACTION_REMOVED,
        TYPING,
        READ_RECEIPT,
        ERROR,
        CONNECTION_ESTABLISHED
    }
}
