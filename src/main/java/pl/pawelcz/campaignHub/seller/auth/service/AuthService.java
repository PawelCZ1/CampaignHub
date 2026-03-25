package pl.pawelcz.campaignHub.seller.auth.service;

import pl.pawelcz.campaignHub.seller.auth.dto.LoginRequest;
import pl.pawelcz.campaignHub.seller.auth.dto.RefreshTokenRequest;
import pl.pawelcz.campaignHub.seller.auth.dto.RegisterRequest;
import pl.pawelcz.campaignHub.seller.auth.dto.TokenResponse;

public interface AuthService {
    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void logout(String refreshToken);
}
