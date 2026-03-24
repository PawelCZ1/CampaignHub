package pl.pawelcz.campaignHub.campaign.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.pawelcz.campaignHub.campaign.entity.CampaignStatus;
import pl.pawelcz.campaignHub.campaign.entity.Campaign;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    boolean existsByProductIdAndStatus(UUID productId, CampaignStatus status);

    boolean existsByProductIdAndStatusAndIdNot(UUID productId, CampaignStatus status, UUID excludedCampaignId);
}
