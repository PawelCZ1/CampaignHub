package pl.pawelcz.campaignHub.campaign.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.pawelcz.campaignHub.campaign.entity.CampaignStatus;
import pl.pawelcz.campaignHub.campaign.entity.Campaign;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    java.util.List<Campaign> findAllBySellerId(UUID sellerId);

    java.util.Optional<Campaign> findByIdAndSellerId(UUID id, UUID sellerId);

    boolean existsByProductIdAndStatus(UUID productId, CampaignStatus status);

    boolean existsByProductIdAndStatusAndIdNot(UUID productId, CampaignStatus status, UUID excludedCampaignId);

    boolean existsByProductIdAndStatusAndSellerId(UUID productId, CampaignStatus status, UUID sellerId);

    boolean existsByProductIdAndStatusAndSellerIdAndIdNot(
        UUID productId,
        CampaignStatus status,
        UUID sellerId,
        UUID excludedCampaignId
    );
}
