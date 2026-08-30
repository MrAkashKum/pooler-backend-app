package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "pb_chat_messages", indexes = {
        @Index(name = "idx_chat_msg_thread", columnList = "thread_id"),
        @Index(name = "idx_chat_msg_sender", columnList = "sender"),
        @Index(name = "idx_chat_msg_entity_id", columnList = "entity_id"),
        @Index(name = "idx_chat_msg_created", columnList = "created_at"),
        @Index(name = "idx_chat_msg_indexed", columnList = "is_indexed")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PbChatMessageEntity extends BaseEntity {

    @Column(name = "entity_id", unique = true, nullable = false)
    private String entityId;

    @Column(name = "thread_id", nullable = false)
    private String threadId;

    @Column(name = "sender", nullable = false)
    private String sender;

    @Column(name = "content", length = 1000, nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "read_by_user_ids")
    @Builder.Default
    private List<String> readByUserIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reactions")
    @Builder.Default
    private Map<String, List<String>> reactions = new HashMap<>();

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "is_indexed")
    @Builder.Default
    private Boolean isIndexed = false;
}
