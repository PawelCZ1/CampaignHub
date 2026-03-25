package pl.pawelcz.campaignHub.seller.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email(message = "Email has invalid format")
    @NotBlank(message = "Email is mandatory")
    String email,
    @NotBlank(message = "Password is mandatory")
    String password
) {
}
