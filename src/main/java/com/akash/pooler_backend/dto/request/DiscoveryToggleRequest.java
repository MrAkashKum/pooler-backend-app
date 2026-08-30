package com.akash.pooler_backend.dto.request;

import com.akash.pooler_backend.enums.DiscoveryMode;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Toggle discovery (ride-share) mode ON or OFF. When turning ON the user
 * MUST send their current coordinates so they can be matched with nearby
 * candidates.
 *
 * @author Akash Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscoveryToggleRequest {

    @NotNull(message = "mode is required")
    private DiscoveryMode mode;

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double currentLatitude;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double currentLongitude;

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double destinationLatitude;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double destinationLongitude;

    @Size(max = 500)
    private String destinationAddress;
}
