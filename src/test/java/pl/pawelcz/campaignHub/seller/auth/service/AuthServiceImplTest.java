package pl.pawelcz.campaignHub.seller.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.pawelcz.campaignHub.campaign.entity.EmeraldAccount;
import pl.pawelcz.campaignHub.campaign.repository.EmeraldAccountRepository;
import pl.pawelcz.campaignHub.campaign.exception.BusinessValidationException;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerAccountRepository sellerAccountRepository;

    @Mock
    private EmeraldAccountRepository emeraldAccountRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
            sellerRepository,
            sellerAccountRepository,
            emeraldAccountRepository,
            refreshTokenRepository,
            passwordEncoder,
            jwtService,
            14
        );
    }

    @Test
    void shouldRegisterSellerAndReturnTokens() {
        RegisterRequest request = new RegisterRequest("  SELLER@example.com ", " Seller One ", "StrongPass123");
        UUID sellerId = UUID.randomUUID();

        when(sellerAccountRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.empty());
        when(sellerRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPass123")).thenReturn("HASH");
        when(jwtService.generateAccessToken(any(), anyString(), any(Integer.class))).thenReturn("ACCESS");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> {
            Seller seller = invocation.getArgument(0);
            seller.setId(sellerId);
            return seller;
        });
        when(sellerAccountRepository.save(any(SellerAccount.class))).thenAnswer(invocation -> {
            SellerAccount account = invocation.getArgument(0);
            account.setId(UUID.randomUUID());
            return account;
        });

        TokenResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("ACCESS");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.sellerId()).isEqualTo(sellerId);
        assertThat(response.email()).isEqualTo("seller@example.com");

        ArgumentCaptor<Seller> sellerCaptor = ArgumentCaptor.forClass(Seller.class);
        verify(sellerRepository).save(sellerCaptor.capture());
        assertThat(sellerCaptor.getValue().getEmail()).isEqualTo("seller@example.com");
        assertThat(sellerCaptor.getValue().getDisplayName()).isEqualTo("Seller One");

        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(emeraldAccountRepository).save(any(EmeraldAccount.class));
    }

    @Test
    void shouldRejectRegisterWhenEmailAlreadyExistsInAccount() {
        when(sellerAccountRepository.findByEmailIgnoreCase("seller@example.com"))
            .thenReturn(Optional.of(SellerAccount.builder().build()));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("seller@example.com", "S", "StrongPass123")))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("Email is already registered");
    }

    @Test
    void shouldRejectRegisterWhenPasswordIsWeak() {
        when(sellerAccountRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.empty());
        when(sellerRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(new RegisterRequest("seller@example.com", "S", "weakpass")))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("Password must include upper-case, lower-case and digit");
    }

    @Test
    void shouldLoginAndReturnTokens() {
        UUID sellerId = UUID.randomUUID();
        Seller seller = Seller.builder().id(sellerId).email("seller@example.com").displayName("S").build();
        SellerAccount account = SellerAccount.builder()
            .seller(seller)
            .email("seller@example.com")
            .passwordHash("HASH")
            .enabled(true)
            .tokenVersion(2)
            .build();

        when(sellerAccountRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("StrongPass123", "HASH")).thenReturn(true);
        when(jwtService.generateAccessToken(sellerId, "seller@example.com", 2)).thenReturn("ACCESS");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        TokenResponse response = authService.login(new LoginRequest("seller@example.com", "StrongPass123"));

        assertThat(response.accessToken()).isEqualTo("ACCESS");
        assertThat(response.sellerId()).isEqualTo(sellerId);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectLoginWhenPasswordInvalid() {
        SellerAccount account = SellerAccount.builder()
            .seller(Seller.builder().id(UUID.randomUUID()).build())
            .email("seller@example.com")
            .passwordHash("HASH")
            .enabled(true)
            .tokenVersion(1)
            .build();

        when(sellerAccountRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("bad", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("seller@example.com", "bad")))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Invalid email or password");
    }

    @Test
    void shouldRefreshWhenTokenValid() {
        Seller seller = Seller.builder().id(UUID.randomUUID()).email("seller@example.com").displayName("S").build();
        SellerAccount account = SellerAccount.builder()
            .seller(seller)
            .email("seller@example.com")
            .passwordHash("HASH")
            .enabled(true)
            .tokenVersion(1)
            .build();
        RefreshToken token = RefreshToken.builder()
            .account(account)
            .expiresAt(Instant.now().plusSeconds(60))
            .createdAt(Instant.now())
            .build();

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString())).thenReturn(Optional.of(token));
        when(jwtService.generateAccessToken(any(), anyString(), any(Integer.class))).thenReturn("ACCESS");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        TokenResponse response = authService.refresh(new RefreshTokenRequest("refresh.raw.value"));

        assertThat(response.accessToken()).isEqualTo("ACCESS");
        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectRefreshWhenTokenMissing() {
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("unknown")))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Refresh token is invalid");
    }

    @Test
    void shouldLogoutByRevokingTokenWhenFound() {
        RefreshToken token = RefreshToken.builder().createdAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString())).thenReturn(Optional.of(token));

        authService.logout("refresh.raw.value");

        assertThat(token.getRevokedAt()).isNotNull();
    }
}
