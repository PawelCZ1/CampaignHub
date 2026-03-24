package pl.pawelcz.campaignHub.campaign.dto;

import java.math.BigDecimal;

public record CampaignWithBalanceResponse(
    CampaignResponse campaign,
    BigDecimal emeraldBalance
) {
}
