package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.SafetyReportStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * A rider-created safety report. Exact live traces are not stored here; only an
 * optional point the rider chooses to attach while submitting the form.
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
        name = "pb_safety_report",
        indexes = {
                @Index(name = "idx_safety_report_reporter", columnList = "reporter_entity_id"),
                @Index(name = "idx_safety_report_status", columnList = "status")
        }
)
public class PbSafetyReportEntity extends BaseEntity {

    @Column(name = "entity_id", nullable = false, unique = true, length = 64)
    private String entityId;

    @Column(name = "reporter_entity_id", nullable = false, length = 64)
    private String reporterEntityId;

    @Column(name = "ride_entity_id", length = 64)
    private String rideEntityId;

    @Column(name = "category", nullable = false, length = 60)
    private String category;

    @Column(name = "details", nullable = false, length = 1000)
    private String details;

    @Column(name = "contact_allowed", nullable = false)
    private boolean contactAllowed;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SafetyReportStatus status;
}
