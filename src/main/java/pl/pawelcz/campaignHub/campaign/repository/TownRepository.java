package pl.pawelcz.campaignHub.campaign.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.pawelcz.campaignHub.campaign.entity.Town;

public interface TownRepository extends JpaRepository<Town, UUID> {
    Optional<Town> findByNameIgnoreCase(String name);
}
