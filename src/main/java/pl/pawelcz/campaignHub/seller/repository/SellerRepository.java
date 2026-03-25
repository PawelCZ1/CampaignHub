package pl.pawelcz.campaignHub.seller.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.pawelcz.campaignHub.seller.entity.Seller;

public interface SellerRepository extends JpaRepository<Seller, UUID> {
    Optional<Seller> findByEmailIgnoreCase(String email);
}