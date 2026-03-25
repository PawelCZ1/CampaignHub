package pl.pawelcz.campaignHub.core;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.pawelcz.campaignHub.campaign.entity.EmeraldAccount;
import pl.pawelcz.campaignHub.campaign.entity.Keyword;
import pl.pawelcz.campaignHub.campaign.entity.Town;
import pl.pawelcz.campaignHub.campaign.repository.EmeraldAccountRepository;
import pl.pawelcz.campaignHub.campaign.repository.KeywordRepository;
import pl.pawelcz.campaignHub.campaign.repository.TownRepository;
import pl.pawelcz.campaignHub.product.entity.Product;
import pl.pawelcz.campaignHub.product.repository.ProductRepository;
import pl.pawelcz.campaignHub.seller.entity.Seller;
import pl.pawelcz.campaignHub.seller.repository.SellerRepository;

@Configuration
public class DataInitializer {

    private static final BigDecimal DEFAULT_EMERALD_BALANCE = new BigDecimal("10000.00");

    @Bean
    CommandLineRunner seedData(
        TownRepository townRepository,
        KeywordRepository keywordRepository,
        EmeraldAccountRepository emeraldAccountRepository,
        ProductRepository productRepository,
        SellerRepository sellerRepository
    ) {
        return args -> {
            if (townRepository.count() == 0) {
                townRepository.saveAll(List.of(
                    Town.builder().name("Warsaw").build(),
                    Town.builder().name("Krakow").build(),
                    Town.builder().name("Gdansk").build(),
                    Town.builder().name("Wroclaw").build(),
                    Town.builder().name("Poznan").build(),
                    Town.builder().name("Lodz").build()
                ));
            }

            if (keywordRepository.count() == 0) {
                keywordRepository.saveAll(List.of(
                    Keyword.builder().value("electronics").build(),
                    Keyword.builder().value("fashion").build(),
                    Keyword.builder().value("sports").build(),
                    Keyword.builder().value("books").build(),
                    Keyword.builder().value("furniture").build(),
                    Keyword.builder().value("garden").build()
                ));
            }

            if (sellerRepository.count() == 0) {
                sellerRepository.saveAll(List.of(
                    Seller.builder().email("seller.one@campaignhub.local").displayName("Seller One").build(),
                    Seller.builder().email("seller.two@campaignhub.local").displayName("Seller Two").build()
                ));
            }

            for (Seller seller : sellerRepository.findAll()) {
                if (emeraldAccountRepository.findBySellerId(seller.getId()) == null) {
                    emeraldAccountRepository.save(
                        EmeraldAccount.builder()
                            .seller(seller)
                            .balance(DEFAULT_EMERALD_BALANCE)
                            .build()
                    );
                }
            }

            if (productRepository.count() == 0) {
                Seller sellerA = sellerRepository.findByEmailIgnoreCase("seller.one@campaignhub.local")
                    .orElseGet(() -> sellerRepository.save(
                        Seller.builder().email("seller.one@campaignhub.local").displayName("Seller One").build()
                    ));

                productRepository.saveAll(List.of(
                    Product.builder().seller(sellerA).name("Laptop Pro 15").description("Premium laptop for professionals").build(),
                    Product.builder().seller(sellerA).name("City Bike X").description("Urban bike with lightweight frame").build(),
                    Product.builder().seller(sellerA).name("Smart Speaker Mini").description("Compact voice assistant speaker").build()
                ));
            }
        };
    }
}
