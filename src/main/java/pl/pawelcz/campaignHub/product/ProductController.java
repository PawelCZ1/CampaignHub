package pl.pawelcz.campaignHub.product;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pawelcz.campaignHub.product.dto.ProductRequest;
import pl.pawelcz.campaignHub.product.dto.ProductResponse;
import pl.pawelcz.campaignHub.product.service.ProductService;
import pl.pawelcz.campaignHub.seller.auth.security.SellerPrincipal;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(@AuthenticationPrincipal SellerPrincipal seller) {
        return ResponseEntity.ok(productService.getAllProducts(seller.sellerId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@AuthenticationPrincipal SellerPrincipal seller, @PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(seller.sellerId(), id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@AuthenticationPrincipal SellerPrincipal seller, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(seller.sellerId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
        @AuthenticationPrincipal SellerPrincipal seller,
        @PathVariable UUID id,
        @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(seller.sellerId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@AuthenticationPrincipal SellerPrincipal seller, @PathVariable UUID id) {
        productService.deleteProduct(seller.sellerId(), id);
        return ResponseEntity.noContent().build();
    }
}
