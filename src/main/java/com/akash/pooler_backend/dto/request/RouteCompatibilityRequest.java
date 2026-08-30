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
public class RouteCompatibilityRequest {

    @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")  private Double userAOriginLat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double userAOriginLng;
    @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")  private Double userADestLat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double userADestLng;

    @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")  private Double userBOriginLat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double userBOriginLng;
    @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")  private Double userBDestLat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double userBDestLng;
}
