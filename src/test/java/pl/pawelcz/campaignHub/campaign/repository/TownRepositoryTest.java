package pl.pawelcz.campaignHub.campaign.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.pawelcz.campaignHub.campaign.entity.Town;

@SpringBootTest
@Transactional
class TownRepositoryTest {

    @Autowired
    private TownRepository townRepository;

    @Test
    void shouldFindByNameIgnoreCase() {
        Town saved = townRepository.findByNameIgnoreCase("Warsaw").orElseThrow();

        assertThat(townRepository.findByNameIgnoreCase("warsaw"))
            .isPresent()
            .get()
            .extracting(Town::getId)
            .isEqualTo(saved.getId());
    }
}
