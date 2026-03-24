package pl.pawelcz.campaignHub.product.repository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.pawelcz.campaignHub.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByNameIgnoreCase(String name);
}
