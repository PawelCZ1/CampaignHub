package pl.pawelcz.campaignHub.seller.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pawelcz.campaignHub.campaign.entity.EmeraldAccount;
import pl.pawelcz.campaignHub.campaign.exception.BusinessValidationException;
import pl.pawelcz.campaignHub.campaign.repository.EmeraldAccountRepository;
import pl.pawelcz.campaignHub.seller.auth.dto.LoginRequest;
import pl.pawelcz.campaignHub.seller.auth.dto.RefreshTokenRequest;
import pl.pawelcz.campaignHub.seller.auth.dto.RegisterRequest;
import pl.pawelcz.campaignHub.seller.auth.dto.TokenResponse;
import pl.pawelcz.campaignHub.seller.auth.entity.RefreshToken;
import pl.pawelcz.campaignHub.seller.auth.entity.SellerAccount;
import pl.pawelcz.campaignHub.seller.auth.exception.UnauthorizedException;
import pl.pawelcz.campaignHub.seller.auth.repository.RefreshTokenRepository;
import pl.pawelcz.campaignHub.seller.auth.repository.SellerAccountRepository;
import pl.pawelcz.campaignHub.seller.auth.security.JwtService;
import pl.pawelcz.campaignHub.seller.entity.Seller;
import pl.pawelcz.campaignHub.seller.repository.SellerRepository;

@Service
public class AuthServiceImpl implements AuthService {

    private static final BigDecimal DEFAULT_EMERALD_BALANCE = new BigDecimal("10000.00");

    private final SellerRepository sellerRepository;
    private final SellerAccountRepository sellerAccountRepository;
    private final EmeraldAccountRepository emeraldAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTokenExpirationDays;

    public AuthServiceImpl(
        SellerRepository sellerRepository,
        SellerAccountRepository sellerAccountRepository,
        EmeraldAccountRepository emeraldAccountRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        @Value("${jwt.refresh-token-expiration-days:14}") long refreshTokenExpirationDays
    ) {
        this.sellerRepository = sellerRepository;
        this.sellerAccountRepository = sellerAccountRepository;
        this.emeraldAccountRepository = emeraldAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (sellerAccountRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new BusinessValidationException("Email is already registered");
        }
        if (sellerRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new BusinessValidationException("Seller profile already exists for this email");
        }

        validatePassword(request.password());

        Seller seller = sellerRepository.save(
            Seller.builder()
                .email(normalizedEmail)
                .displayName(request.displayName().trim())
                .build()
        );

        SellerAccount account = sellerAccountRepository.save(
            SellerAccount.builder()
                .seller(seller)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(true)
                .tokenVersion(1)
                .build()
        );

            emeraldAccountRepository.save(
                EmeraldAccount.builder()
                .seller(seller)
                .balance(DEFAULT_EMERALD_BALANCE)
                .build()
            );

        return issueTokens(account);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        SellerAccount account = sellerAccountRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!account.isEnabled() || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return issueTokens(account);
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());

        String hash = hashToken(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
            .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired");
        }

        SellerAccount account = stored.getAccount();
        if (!account.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        stored.setRevokedAt(Instant.now());

        return issueTokens(account);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        String hash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
            .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    private TokenResponse issueTokens(SellerAccount account) {
        String accessToken = jwtService.generateAccessToken(
            account.getSeller().getId(),
            account.getEmail(),
            account.getTokenVersion()
        );

        String refreshTokenRaw = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken refreshToken = RefreshToken.builder()
            .account(account)
            .tokenHash(hashToken(refreshTokenRaw))
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS))
            .build();
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
            accessToken,
            refreshTokenRaw,
            "Bearer",
            jwtService.getAccessTokenExpirationSeconds(),
            account.getSeller().getId(),
            account.getEmail()
        );
    }

    private void validatePassword(String password) {
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasUpper || !hasLower || !hasDigit) {
            throw new BusinessValidationException("Password must include upper-case, lower-case and digit");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
