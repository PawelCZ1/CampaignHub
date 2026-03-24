package pl.pawelcz.campaignHub.product.service;

import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import pl.pawelcz.campaignHub.core.NotFoundException;
import pl.pawelcz.campaignHub.product.dto.ProductRequest;
import pl.pawelcz.campaignHub.product.dto.ProductResponse;
import pl.pawelcz.campaignHub.product.entity.Product;
import pl.pawelcz.campaignHub.product.repository.ProductRepository;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(ProductResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
            .name(request.name().trim())
            .description(normalizeDescription(request.description()))
            .build();
        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));

        product.setName(request.name().trim());
        product.setDescription(normalizeDescription(request.description()));

        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));
        productRepository.delete(product);
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
