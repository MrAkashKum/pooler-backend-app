package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Private rider feedback submitted from the mobile profile/settings area.
 *
 * @author Akash Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFeedbackRequest {

    @NotBlank(message = "emotion is required")
    @Size(max = 40, message = "emotion must be at most 40 characters")
    private String emotion;

    @NotBlank(message = "subject is required")
    @Size(max = 80, message = "subject must be at most 80 characters")
    private String subject;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be at least 1")
    @Max(value = 5, message = "rating must be at most 5")
    private Integer rating;

    @Size(max = 1000, message = "message must be at most 1000 characters")
    private String message;
}
