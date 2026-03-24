package pl.pawelcz.campaignHub.product.service;

import java.util.List;
import java.util.UUID;
import pl.pawelcz.campaignHub.product.dto.ProductRequest;
import pl.pawelcz.campaignHub.product.dto.ProductResponse;

public interface ProductService {
    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(UUID id);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(UUID id, ProductRequest request);

    void deleteProduct(UUID id);
}
