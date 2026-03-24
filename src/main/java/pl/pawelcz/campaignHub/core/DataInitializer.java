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

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(
        TownRepository townRepository,
        KeywordRepository keywordRepository,
        EmeraldAccountRepository emeraldAccountRepository,
        ProductRepository productRepository
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

            if (emeraldAccountRepository.count() == 0) {
                emeraldAccountRepository.save(EmeraldAccount.builder().balance(new BigDecimal("10000.00")).build());
            }

            if (productRepository.count() == 0) {
                productRepository.saveAll(List.of(
                    Product.builder().name("Laptop Pro 15").description("Premium laptop for professionals").build(),
                    Product.builder().name("City Bike X").description("Urban bike with lightweight frame").build(),
                    Product.builder().name("Smart Speaker Mini").description("Compact voice assistant speaker").build()
                ));
            }
        };
    }
}
