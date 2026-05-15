package dto;

import jakarta.validation.constraints.NotBlank;

public record SaveLocationRequest(
        @NotBlank String locationName
) {
}