package com.akash.pooler_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

/**
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="pb_audit_logs",
        indexes={@Index(name="idx_al_entity_id",columnList="entity_id"),
                @Index(name="idx_al_action",columnList="action"),
                @Index(name="idx_al_created",columnList="createdAt")})
public class PbAuditLogEntity extends BaseEntity{

    @Column(name="entity_id")
    private String entityId;

    @Column(name = "action", nullable=false, length=100)
    private String action;

    @Column(name = "details", length=500)
    private String details;

    @Column(name = "ip_address", length=45)
    private String ipAddress;

    @Column(name = "user_agent", length=200)
    private String userAgent;

    @Column(name = "platform", length=50)
    private String platform;


    public static PbAuditLogEntity of(String entityId, String action, String details, String ip) {
        return PbAuditLogEntity.builder().entityId(entityId).action(action).details(details).ipAddress(ip).build();
    }
}
