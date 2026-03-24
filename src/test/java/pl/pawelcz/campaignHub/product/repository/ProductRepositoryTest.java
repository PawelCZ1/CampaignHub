package pl.pawelcz.campaignHub.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.pawelcz.campaignHub.product.entity.Product;

@SpringBootTest
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldFindByNameIgnoreCase() {
        Product product = productRepository.findByNameIgnoreCase("Laptop Pro 15").orElseThrow();

        assertThat(productRepository.findByNameIgnoreCase("laptop pro 15"))
            .isPresent()
            .get()
            .extracting(Product::getId)
            .isEqualTo(product.getId());
    }
}
