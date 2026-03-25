package pl.pawelcz.campaignHub.seller.auth.dto;

import java.util.UUID;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UUID sellerId,
    String email
) {
}
