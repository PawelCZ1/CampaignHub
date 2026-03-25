package pl.pawelcz.campaignHub.seller.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void shouldGenerateAndParseAccessToken() {
        JwtService jwtService = new JwtService(
            "campaignhub-jwt-secret-key-change-in-prod-32bytes-min",
            "campaignHub",
            900
        );

        UUID sellerId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(sellerId, "seller@example.com", 3);

        JwtService.AccessTokenClaims claims = jwtService.parseAccessToken(token);

        assertThat(claims.sellerId()).isEqualTo(sellerId);
        assertThat(claims.email()).isEqualTo("seller@example.com");
        assertThat(claims.tokenVersion()).isEqualTo(3);
    }

    @Test
    void shouldRejectTokenWithDifferentIssuer() {
        JwtService issuerA = new JwtService(
            "campaignhub-jwt-secret-key-change-in-prod-32bytes-min",
            "campaignHub",
            900
        );
        JwtService issuerB = new JwtService(
            "campaignhub-jwt-secret-key-change-in-prod-32bytes-min",
            "otherIssuer",
            900
        );

        String token = issuerA.generateAccessToken(UUID.randomUUID(), "seller@example.com", 1);

        assertThatThrownBy(() -> issuerB.parseAccessToken(token))
            .isInstanceOf(JwtException.class);
    }
}
