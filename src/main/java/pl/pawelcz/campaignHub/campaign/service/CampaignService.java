package pl.pawelcz.campaignHub.campaign.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import pl.pawelcz.campaignHub.campaign.dto.CampaignRequest;
import pl.pawelcz.campaignHub.campaign.dto.CampaignResponse;
import pl.pawelcz.campaignHub.campaign.dto.CampaignWithBalanceResponse;

public interface CampaignService {
    List<CampaignResponse> getAllCampaigns(UUID sellerId);

    CampaignResponse getCampaignById(UUID sellerId, UUID id);

    CampaignWithBalanceResponse createCampaign(UUID sellerId, CampaignRequest request);

    CampaignWithBalanceResponse updateCampaign(UUID sellerId, UUID id, CampaignRequest request);

    void deleteCampaign(UUID sellerId, UUID id);

    List<String> searchKeywords(String query);

    List<String> getTowns();

    BigDecimal getEmeraldBalance(UUID sellerId);
}
