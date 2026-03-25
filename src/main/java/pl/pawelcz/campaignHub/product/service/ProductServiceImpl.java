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
import pl.pawelcz.campaignHub.seller.entity.Seller;
import pl.pawelcz.campaignHub.seller.repository.SellerRepository;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;

    public ProductServiceImpl(ProductRepository productRepository, SellerRepository sellerRepository) {
        this.productRepository = productRepository;
        this.sellerRepository = sellerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(UUID sellerId) {
        return productRepository.findAllBySellerId(sellerId).stream().map(ProductResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID sellerId, UUID id) {
        Product product = productRepository.findByIdAndSellerId(id, sellerId)
            .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(UUID sellerId, ProductRequest request) {
        Product product = Product.builder()
            .seller(requireSeller(sellerId))
            .name(request.name().trim())
            .description(normalizeDescription(request.description()))
            .build();
        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID sellerId, UUID id, ProductRequest request) {
        Product product = productRepository.findByIdAndSellerId(id, sellerId)
            .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));

        product.setName(request.name().trim());
        product.setDescription(normalizeDescription(request.description()));

        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(UUID sellerId, UUID id) {
        Product product = productRepository.findByIdAndSellerId(id, sellerId)
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

    private Seller requireSeller(UUID sellerId) {
        return sellerRepository.findById(sellerId)
            .orElseThrow(() -> new NotFoundException("Seller with id " + sellerId + " not found"));
    }
}
