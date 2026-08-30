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
public class CreateSafetyReportRequest {

    @Size(max = 64, message = "rideEntityId must be at most 64 characters")
    private String rideEntityId;

    @NotBlank(message = "category is required")
    @Size(max = 60, message = "category must be at most 60 characters")
    private String category;

    @NotBlank(message = "details are required")
    @Size(min = 10, max = 1000, message = "details must be between 10 and 1000 characters")
    private String details;

    private Boolean contactAllowed;

    @DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "latitude must be <= 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "longitude must be <= 180")
    private Double longitude;
}
