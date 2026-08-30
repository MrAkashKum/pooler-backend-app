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
public class MidpointRequest {

    @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")  private Double userALatitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double userALongitude;
    @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")  private Double userBLatitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double userBLongitude;
}
