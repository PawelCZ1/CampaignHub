package pl.pawelcz.campaignHub.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pawelcz.campaignHub.core.NotFoundException;
import pl.pawelcz.campaignHub.product.dto.ProductRequest;
import pl.pawelcz.campaignHub.product.dto.ProductResponse;
import pl.pawelcz.campaignHub.product.entity.Product;
import pl.pawelcz.campaignHub.product.repository.ProductRepository;
import pl.pawelcz.campaignHub.seller.entity.Seller;
import pl.pawelcz.campaignHub.seller.repository.SellerRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SellerRepository sellerRepository;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, sellerRepository);
    }

    @Test
    void shouldGetAllProducts() {
        UUID sellerId = UUID.randomUUID();
        Seller seller = Seller.builder().id(sellerId).email("seller@test.local").displayName("Seller").build();
        when(productRepository.findAllBySellerId(sellerId)).thenReturn(List.of(
            Product.builder().id(UUID.randomUUID()).seller(seller).name("Laptop Pro 15").description("A").build(),
            Product.builder().id(UUID.randomUUID()).seller(seller).name("City Bike X").description("B").build()
        ));

        List<ProductResponse> result = productService.getAllProducts(sellerId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductResponse::name).containsExactly("Laptop Pro 15", "City Bike X");
    }

    @Test
    void shouldGetProductById() {
        UUID id = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Seller seller = Seller.builder().id(sellerId).email("seller@test.local").displayName("Seller").build();
        when(productRepository.findByIdAndSellerId(id, sellerId)).thenReturn(Optional.of(Product.builder().id(id).seller(seller).name("Laptop Pro 15").description("Desc").build()));

        ProductResponse result = productService.getProductById(sellerId, id);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Laptop Pro 15");
    }

    @Test
    void shouldThrowWhenProductNotFoundOnGet() {
        UUID id = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        when(productRepository.findByIdAndSellerId(id, sellerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(sellerId, id))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Product with id " + id + " not found");
    }

    @Test
    void shouldCreateProductAndTrimFields() {
        UUID id = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Seller seller = Seller.builder().id(sellerId).email("seller@test.local").displayName("Seller").build();
        Product saved = Product.builder().id(id).seller(seller).name("Gaming Monitor").description("165Hz").build();
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse result = productService.createProduct(sellerId, new ProductRequest("  Gaming Monitor  ", " 165Hz "));

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Gaming Monitor");
        assertThat(result.description()).isEqualTo("165Hz");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldNormalizeBlankDescriptionToNullOnCreate() {
        UUID id = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Seller seller = Seller.builder().id(sellerId).email("seller@test.local").displayName("Seller").build();
        Product saved = Product.builder().id(id).seller(seller).name("Keyboard").description(null).build();
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse result = productService.createProduct(sellerId, new ProductRequest("Keyboard", "   "));

        assertThat(result.description()).isNull();
    }

    @Test
    void shouldUpdateProduct() {
        UUID id = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Seller seller = Seller.builder().id(sellerId).email("seller@test.local").displayName("Seller").build();
        Product existing = Product.builder().id(id).seller(seller).name("Old").description("Old desc").build();
        Product saved = Product.builder().id(id).seller(seller).name("New").description("New desc").build();

        when(productRepository.findByIdAndSellerId(id, sellerId)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(saved);

        ProductResponse result = productService.updateProduct(sellerId, id, new ProductRequest("  New  ", " New desc "));

        assertThat(result.name()).isEqualTo("New");
        assertThat(result.description()).isEqualTo("New desc");
    }

    @Test
    void shouldThrowWhenProductNotFoundOnUpdate() {
        UUID id = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        when(productRepository.findByIdAndSellerId(id, sellerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(sellerId, id, new ProductRequest("Name", "Desc")))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Product with id " + id + " not found");
    }

    @Test
    void shouldDeleteProduct() {
        UUID id = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Seller seller = Seller.builder().id(UUID.randomUUID()).email("seller@test.local").displayName("Seller").build();
        Product existing = Product.builder().id(id).seller(seller).name("Laptop Pro 15").build();
        when(productRepository.findByIdAndSellerId(id, sellerId)).thenReturn(Optional.of(existing));

        productService.deleteProduct(sellerId, id);

        verify(productRepository).delete(existing);
    }

    @Test
    void shouldThrowWhenProductNotFoundOnDelete() {
        UUID id = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        when(productRepository.findByIdAndSellerId(id, sellerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(sellerId, id))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Product with id " + id + " not found");
    }
}
