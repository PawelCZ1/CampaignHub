package pl.pawelcz.campaignHub.campaign.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.pawelcz.campaignHub.campaign.entity.EmeraldAccount;
import pl.pawelcz.campaignHub.seller.repository.SellerRepository;

@SpringBootTest
@Transactional
class EmeraldAccountRepositoryTest {

    @Autowired
    private EmeraldAccountRepository emeraldAccountRepository;

    @Autowired
    private SellerRepository sellerRepository;

    private java.util.UUID sellerId;

    @BeforeEach
    void setUp() {
        sellerId = sellerRepository.findByEmailIgnoreCase("seller.one@campaignhub.local").orElseThrow().getId();
    }

    @Test
    void shouldFindAccountBySellerId() {
        EmeraldAccount result = emeraldAccountRepository.findBySellerId(sellerId);

        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.getSeller().getId()).isEqualTo(sellerId);
    }
}
