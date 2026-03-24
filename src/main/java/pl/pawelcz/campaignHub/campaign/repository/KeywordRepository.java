package pl.pawelcz.campaignHub.campaign.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.pawelcz.campaignHub.campaign.entity.Keyword;

public interface KeywordRepository extends JpaRepository<Keyword, UUID> {
    List<Keyword> findByValueContainingIgnoreCase(String query);
    Optional<Keyword> findByValueIgnoreCase(String value);
}
