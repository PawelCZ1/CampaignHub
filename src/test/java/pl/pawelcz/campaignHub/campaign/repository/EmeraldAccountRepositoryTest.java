package pl.pawelcz.campaignHub.campaign.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.pawelcz.campaignHub.campaign.entity.EmeraldAccount;

@SpringBootTest
@Transactional
class EmeraldAccountRepositoryTest {

    @Autowired
    private EmeraldAccountRepository emeraldAccountRepository;

    @Test
    void shouldFindTopByOrderByIdAscWhenSingleAccountExists() {
        EmeraldAccount result = emeraldAccountRepository.findTopByOrderByIdAsc();

        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isGreaterThan(BigDecimal.ZERO);
    }
}
