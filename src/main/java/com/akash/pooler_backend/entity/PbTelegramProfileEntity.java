package com.akash.pooler_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "pb_telegram_profiles", indexes = {
        @Index(name = "idx_telegram_entity_id", columnList = "entity_id"),
        @Index(name = "idx_telegram_user_id", columnList = "user_id", unique = true),
        @Index(name = "idx_telegram_handle", columnList = "telegram_handle", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PbTelegramProfileEntity extends BaseEntity {

    @Column(name = "entity_id", unique = true, nullable = false)
    private String entityId;

    @Column(name = "user_id", unique = true, nullable = false)
    private String userEntityId;

    @Column(name = "telegram_handle", unique = true)
    private String telegramHandle;

    @Column(name = "telegram_phone_number")
    private String telegramPhoneNumber;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;
}
