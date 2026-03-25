package pl.pawelcz.campaignHub.seller.auth.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.pawelcz.campaignHub.seller.auth.entity.SellerAccount;

public interface SellerAccountRepository extends JpaRepository<SellerAccount, UUID> {
    Optional<SellerAccount> findByEmailIgnoreCase(String email);

    Optional<SellerAccount> findBySellerId(UUID sellerId);
}
