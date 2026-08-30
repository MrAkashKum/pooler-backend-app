package com.akash.pooler_backend.dto.request;

import com.akash.pooler_backend.enums.LocationAlias;
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
public class SaveLocationRequest {

    @NotNull(message = "alias is required")
    private LocationAlias alias;

    @Size(max = 120, message = "label must be at most 120 characters")
    private String label;

    @Size(max = 500, message = "address must be at most 500 characters")
    private String address;

    @NotNull(message = "latitude is required")
    @DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @DecimalMax(value = "90.0",  message = "latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "longitude is required")
    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "longitude must be <= 180")
    private Double longitude;
}
