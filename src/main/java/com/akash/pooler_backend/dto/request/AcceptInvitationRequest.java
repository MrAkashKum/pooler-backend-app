package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Receiver attaches their current location and intended destination
 * when accepting. The Common Pickup Hub is computed at this point.
 *
 * @author Akash Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptInvitationRequest {

    @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double receiverLatitude;

    @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double receiverLongitude;

    @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double receiverDestinationLatitude;

    @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double receiverDestinationLongitude;

    @Size(max = 500)
    private String receiverDestinationAddress;
}
