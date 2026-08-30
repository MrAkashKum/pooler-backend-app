package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.Gender;
import com.akash.pooler_backend.enums.MatchPreference;
import com.akash.pooler_backend.enums.Role;
import com.akash.pooler_backend.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(
        name = "pb_user",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        }
)
public class PbUserEntity extends BaseEntity implements UserDetails {

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "entity_id")
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(nullable=false)
    private String passwordHash;

    @Column(nullable=false,length=100)
    private String firstName;

    @Column(nullable=false,length=100)
    private String lastName;

    @Column(length=500)
    private String profilePictureUrl;

    @Column(length=500)
    private String paymentQrCodeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Gender gender = Gender.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MatchPreference matchPreference = MatchPreference.ANY;

    @Column(length = 120)
    private String emergencyContactName;

    @Column(length = 32)
    private String emergencyContactPhone;

    @Column(length = 300)
    private String emergencyMessage;

    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    private Instant lastLoginAt;

    @Builder.Default
    private int failedLoginAttempts = 0;

    private Instant lockedUntil;

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(Instant.now()); }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0; this.lockedUntil = null;
    }

}
