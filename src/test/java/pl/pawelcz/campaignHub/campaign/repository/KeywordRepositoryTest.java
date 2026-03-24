package pl.pawelcz.campaignHub.campaign.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.pawelcz.campaignHub.campaign.entity.Keyword;

@SpringBootTest
@Transactional
class KeywordRepositoryTest {

    @Autowired
    private KeywordRepository keywordRepository;

    @Test
    void shouldFindByValueIgnoreCase() {
        Keyword saved = keywordRepository.findByValueIgnoreCase("electronics").orElseThrow();

        assertThat(keywordRepository.findByValueIgnoreCase("ELECTRONICS"))
            .isPresent()
            .get()
            .extracting(Keyword::getId)
            .isEqualTo(saved.getId());
    }

    @Test
    void shouldFindByValueContainingIgnoreCase() {
        keywordRepository.save(Keyword.builder().value("ebooks-test").build());

        List<Keyword> result = keywordRepository.findByValueContainingIgnoreCase("book");

        assertThat(result)
            .extracting(Keyword::getValue)
            .contains("books", "ebooks-test");
    }
}
