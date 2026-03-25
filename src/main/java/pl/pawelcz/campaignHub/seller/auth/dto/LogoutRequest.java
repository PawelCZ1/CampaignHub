package pl.pawelcz.campaignHub.seller.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
    @NotBlank(message = "Refresh token is mandatory")
    String refreshToken
) {
}
