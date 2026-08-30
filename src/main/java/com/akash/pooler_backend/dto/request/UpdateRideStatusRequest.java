package com.akash.pooler_backend.dto.request;

import com.akash.pooler_backend.enums.RideStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * @author Akash Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRideStatusRequest {

    @NotNull(message = "status is required")
    private RideStatus status;
}
