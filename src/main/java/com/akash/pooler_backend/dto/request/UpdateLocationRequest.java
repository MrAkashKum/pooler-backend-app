package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Partial update — all fields are optional. Validation is range-only.
 *
 * @author Akash Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLocationRequest {

    @Size(max = 120, message = "label must be at most 120 characters")
    private String label;

    @Size(max = 500, message = "address must be at most 500 characters")
    private String address;

    @DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @DecimalMax(value = "90.0",  message = "latitude must be <= 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "longitude must be <= 180")
    private Double longitude;
}
