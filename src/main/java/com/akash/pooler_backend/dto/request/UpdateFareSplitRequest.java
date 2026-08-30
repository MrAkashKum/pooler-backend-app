package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFareSplitRequest {

    @DecimalMin(value = "0.01", message = "Total fare must be greater than zero")
    @DecimalMax(value = "99999.00", message = "Total fare is too high")
    @NotNull(message = "Total fare is required")
    private Double totalFare;

    @Size(max = 12)
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must use a 3-letter code")
    private String currency;

    @NotBlank(message = "Cab provider is required")
    @Size(max = 60)
    private String provider;
}
