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
public class SendRideInvitationRequest {

    @NotBlank(message = "receiverEntityId is required")
    private String receiverEntityId;

    @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double senderLatitude;

    @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double senderLongitude;

    @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double senderDestinationLatitude;

    @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double senderDestinationLongitude;

    @Size(max = 500)
    private String senderDestinationAddress;

    @Size(max = 500)
    private String message;

    /** Invitation TTL — defaults to 5 minutes when null. */
    @Min(60) @Max(1800)
    private Integer ttlSeconds;
}
