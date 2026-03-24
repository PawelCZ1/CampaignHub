package pl.pawelcz.campaignHub.campaign.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import pl.pawelcz.campaignHub.campaign.entity.Campaign;
import pl.pawelcz.campaignHub.campaign.entity.CampaignStatus;
import pl.pawelcz.campaignHub.campaign.entity.Keyword;

public record CampaignResponse(
    UUID id,
    String name,
    Set<String> keywords,
    BigDecimal bidAmount,
    BigDecimal campaignFund,
    CampaignStatus status,
    String town,
    Integer radiusInKm
) {
    public static CampaignResponse fromEntity(Campaign campaign) {
        return new CampaignResponse(
            campaign.getId(),
            campaign.getName(),
            campaign.getKeywords().stream().map(Keyword::getValue).collect(Collectors.toSet()),
            campaign.getBidAmount(),
            campaign.getCampaignFund(),
            campaign.getStatus(),
            campaign.getTown(),
            campaign.getRadiusInKm()
        );
    }
}
