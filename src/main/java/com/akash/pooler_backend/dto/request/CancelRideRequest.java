package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * @author Akash Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelRideRequest {

    @Size(max = 500, message = "reason must be at most 500 characters")
    private String reason;
}
