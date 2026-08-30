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
public class LiveLocationUpdateRequest {

    @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")  private Double latitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double longitude;

    @DecimalMin("0.0") @DecimalMax("360.0")
    private Double headingDegrees;

    @DecimalMin("0.0")
    private Double speedKmh;

    @DecimalMin("0.0")
    private Double accuracyMeters;
}
