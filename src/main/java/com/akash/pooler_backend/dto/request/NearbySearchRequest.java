package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * @author Akash Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbySearchRequest {

    @NotNull
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double longitude;

    /** Radius in kilometres (default 3 km). */
    @DecimalMin(value = "0.1") @DecimalMax(value = "25.0")
    @Builder.Default
    private Double radiusKm = 3.0;

    /** Optional: only return users heading roughly the same direction. */
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double destinationLatitude;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double destinationLongitude;
}
