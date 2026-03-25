package pl.pawelcz.campaignHub.seller.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Email(message = "Email has invalid format")
    @NotBlank(message = "Email is mandatory")
    String email,
    @NotBlank(message = "Display name is mandatory")
    String displayName,
    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {
}
