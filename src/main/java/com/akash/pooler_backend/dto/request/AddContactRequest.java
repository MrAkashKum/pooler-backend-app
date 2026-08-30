package com.akash.pooler_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class AddContactRequest {

    @NotBlank(message = "contactUserEntityId is required")
    private String contactUserEntityId;

    @Size(max = 120, message = "nickname must be at most 120 characters")
    private String nickname;

    private boolean favorite;
}
