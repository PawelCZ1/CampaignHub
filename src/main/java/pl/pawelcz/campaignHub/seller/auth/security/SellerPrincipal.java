package pl.pawelcz.campaignHub.seller.auth.security;

import java.util.UUID;

public record SellerPrincipal(UUID sellerId, String email) {
}
