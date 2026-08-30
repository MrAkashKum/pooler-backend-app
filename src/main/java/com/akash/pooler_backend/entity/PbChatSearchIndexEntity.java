package com.akash.pooler_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "pb_chat_search_indexes", indexes = {
        @Index(name = "idx_search_idx_entity_id", columnList = "entity_id"),
        @Index(name = "idx_search_idx_thread", columnList = "thread_id", unique = true),
        @Index(name = "idx_search_idx_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PbChatSearchIndexEntity extends BaseEntity {

    @Column(name = "entity_id", unique = true, nullable = false)
    private String entityId;

    @Column(name = "thread_id", nullable = false)
    private String threadId;

    @Column(name = "message_text")
    private String messageText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "message_ids")
    private List<String> messageIds;
}
