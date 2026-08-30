package com.akash.pooler_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Private app feedback. This is intentionally not exposed to other riders.
 *
 * @author Akash Kumar
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(
        name = "pb_feedback",
        indexes = {
                @Index(name = "idx_feedback_submitter", columnList = "submitter_entity_id"),
                @Index(name = "idx_feedback_created", columnList = "created_at")
        }
)
public class PbFeedbackEntity extends BaseEntity {

    @Column(name = "entity_id", nullable = false, unique = true, length = 64)
    private String entityId;

    @Column(name = "submitter_entity_id", nullable = false, length = 64)
    private String submitterEntityId;

    @Column(name = "emotion", nullable = false, length = 40)
    private String emotion;

    @Column(name = "subject", nullable = false, length = 80)
    private String subject;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "platform", length = 30)
    private String platform;

    @Column(name = "app_version", length = 40)
    private String appVersion;
}
