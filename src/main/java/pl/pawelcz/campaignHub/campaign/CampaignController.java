package pl.pawelcz.campaignHub.campaign;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.pawelcz.campaignHub.campaign.dto.CampaignRequest;
import pl.pawelcz.campaignHub.campaign.dto.CampaignResponse;
import pl.pawelcz.campaignHub.campaign.dto.CampaignWithBalanceResponse;
import pl.pawelcz.campaignHub.campaign.dto.EmeraldBalanceResponse;
import pl.pawelcz.campaignHub.campaign.service.CampaignService;
import pl.pawelcz.campaignHub.seller.auth.security.SellerPrincipal;

@RestController
@RequestMapping("/api")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping("/campaigns")
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns(@AuthenticationPrincipal SellerPrincipal seller) {
        return ResponseEntity.ok(campaignService.getAllCampaigns(seller.sellerId()));
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<CampaignResponse> getCampaign(@AuthenticationPrincipal SellerPrincipal seller, @PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.getCampaignById(seller.sellerId(), id));
    }

    @PostMapping("/campaigns")
    public ResponseEntity<CampaignWithBalanceResponse> createCampaign(
        @AuthenticationPrincipal SellerPrincipal seller,
        @Valid @RequestBody CampaignRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.createCampaign(seller.sellerId(), request));
    }

    @PutMapping("/campaigns/{id}")
    public ResponseEntity<CampaignWithBalanceResponse> updateCampaign(
        @AuthenticationPrincipal SellerPrincipal seller,
        @PathVariable UUID id,
        @Valid @RequestBody CampaignRequest request
    ) {
        return ResponseEntity.ok(campaignService.updateCampaign(seller.sellerId(), id, request));
    }

    @DeleteMapping("/campaigns/{id}")
    public ResponseEntity<Void> deleteCampaign(@AuthenticationPrincipal SellerPrincipal seller, @PathVariable UUID id) {
        campaignService.deleteCampaign(seller.sellerId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/keywords")
    public ResponseEntity<List<String>> getKeywords(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(campaignService.searchKeywords(query));
    }

    @GetMapping("/towns")
    public ResponseEntity<List<String>> getTowns() {
        return ResponseEntity.ok(campaignService.getTowns());
    }

    @GetMapping("/emerald-account/balance")
    public ResponseEntity<EmeraldBalanceResponse> getBalance(@AuthenticationPrincipal SellerPrincipal seller) {
        return ResponseEntity.ok(new EmeraldBalanceResponse(campaignService.getEmeraldBalance(seller.sellerId())));
    }
}
