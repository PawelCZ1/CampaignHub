package pl.pawelcz.campaignHub.campaign.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.pawelcz.campaignHub.campaign.entity.Campaign;
import pl.pawelcz.campaignHub.campaign.entity.CampaignStatus;
import pl.pawelcz.campaignHub.campaign.entity.Keyword;

@SpringBootTest
@Transactional
class CampaignRepositoryTest {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Test
    void shouldPersistCampaignWithKeywords() {
        Keyword books = keywordRepository.findByValueIgnoreCase("books").orElseThrow();
        Keyword electronics = keywordRepository.findByValueIgnoreCase("electronics").orElseThrow();

        Campaign campaign = Campaign.builder()
            .name("School Promo")
            .keywords(Set.of(books, electronics))
            .bidAmount(new BigDecimal("2.50"))
            .campaignFund(new BigDecimal("300.00"))
            .status(CampaignStatus.ON)
            .town("Warsaw")
            .radiusInKm(20)
            .build();

        Campaign saved = campaignRepository.save(campaign);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getKeywords()).hasSize(2);
        assertThat(saved.getKeywords()).extracting(Keyword::getValue).containsExactlyInAnyOrder("books", "electronics");
    }
}
