package pl.pawelcz.campaignHub.seller.auth.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.pawelcz.campaignHub.seller.auth.repository.SellerAccountRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SellerAccountRepository sellerAccountRepository;

    public JwtAuthenticationFilter(JwtService jwtService, SellerAccountRepository sellerAccountRepository) {
        this.jwtService = jwtService;
        this.sellerAccountRepository = sellerAccountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            JwtService.AccessTokenClaims claims = jwtService.parseAccessToken(token);

            sellerAccountRepository.findBySellerId(claims.sellerId())
                .filter(account -> account.getTokenVersion() == claims.tokenVersion())
                .filter(account -> account.isEnabled())
                .ifPresent(account -> {
                    SellerPrincipal principal = new SellerPrincipal(claims.sellerId(), claims.email());
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
        } catch (JwtException ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
