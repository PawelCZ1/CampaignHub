package pl.pawelcz.campaignHub.seller.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final long accessTokenExpirationSeconds;

    public JwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.issuer:campaignHub}") String issuer,
        @Value("${jwt.access-token-expiration-seconds:900}") long accessTokenExpirationSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    public String generateAccessToken(UUID sellerId, String email, int tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(sellerId.toString())
            .claim("email", email)
            .claim("tokenVersion", tokenVersion)
            .claim("type", "access")
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(accessTokenExpirationSeconds)))
            .signWith(key)
            .compact();
    }

    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (!"access".equals(claims.get("type", String.class))) {
            throw new JwtException("Unexpected token type");
        }

        UUID sellerId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        Integer tokenVersion = claims.get("tokenVersion", Integer.class);

        if (email == null || tokenVersion == null) {
            throw new JwtException("Token is missing required claims");
        }

        return new AccessTokenClaims(sellerId, email, tokenVersion);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public record AccessTokenClaims(UUID sellerId, String email, int tokenVersion) {
    }
}
