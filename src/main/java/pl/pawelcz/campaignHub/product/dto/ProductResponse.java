package pl.pawelcz.campaignHub.product.dto;

import java.util.UUID;
import pl.pawelcz.campaignHub.product.entity.Product;

public record ProductResponse(
    UUID id,
    UUID sellerId,
    String name,
    String description
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getSeller().getId(),
            product.getName(),
            product.getDescription()
        );
    }
}
