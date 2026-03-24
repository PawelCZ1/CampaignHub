package pl.pawelcz.campaignHub.campaign.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import pl.pawelcz.campaignHub.campaign.entity.CampaignStatus;

public record CampaignRequest(
    @NotNull(message = "Product id is mandatory")
    UUID productId,
    @NotBlank(message = "Campaign name is mandatory")
    String name,
    @NotEmpty(message = "At least one keyword is mandatory")
    Set<@NotBlank(message = "Keyword cannot be blank") String> keywords,
    @NotNull(message = "Bid amount is mandatory")
    @DecimalMin(value = "0.01", message = "Bid amount must be at least 0.01")
    BigDecimal bidAmount,
    @NotNull(message = "Campaign fund is mandatory")
    @DecimalMin(value = "0.01", message = "Campaign fund must be at least 0.01")
    BigDecimal campaignFund,
    @NotNull(message = "Status is mandatory")
    CampaignStatus status,
    String town,
    @NotNull(message = "Radius is mandatory")
    @Positive(message = "Radius must be greater than 0")
    Integer radiusInKm
) {
}
