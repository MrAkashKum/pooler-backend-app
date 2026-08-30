package com.akash.pooler_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * @author Akash kumar
 *
 */

@Entity
@Table(name = "pb_chat_archives", indexes = {
        @Index(name = "idx_archive_entity_id", columnList = "entity_id"),
        @Index(name = "idx_archive_thread_id", columnList = "thread_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PbChatArchiveEntity extends BaseEntity {

    @Column(name = "entity_id", unique = true, nullable = false)
    private String entityId;

    @Column(name = "thread_id", nullable = false, unique = true)
    private String threadId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "archive_data", nullable = false)
    private Object archiveData;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @CreationTimestamp
    @Column(name = "archived_at", nullable = false, updatable = false)
    private Instant archivedAt;
}
