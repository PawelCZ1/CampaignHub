package pl.pawelcz.campaignHub.product.service;

import java.util.List;
import java.util.UUID;
import pl.pawelcz.campaignHub.product.dto.ProductRequest;
import pl.pawelcz.campaignHub.product.dto.ProductResponse;

public interface ProductService {
    List<ProductResponse> getAllProducts(UUID sellerId);

    ProductResponse getProductById(UUID sellerId, UUID id);

    ProductResponse createProduct(UUID sellerId, ProductRequest request);

    ProductResponse updateProduct(UUID sellerId, UUID id, ProductRequest request);

    void deleteProduct(UUID sellerId, UUID id);
}
