package pl.pawelcz.campaignHub.campaign.service;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pl.pawelcz.campaignHub.campaign.dto.CampaignRequest;
import pl.pawelcz.campaignHub.campaign.dto.CampaignResponse;
import pl.pawelcz.campaignHub.campaign.dto.CampaignWithBalanceResponse;
import pl.pawelcz.campaignHub.campaign.entity.Campaign;
import pl.pawelcz.campaignHub.campaign.entity.EmeraldAccount;
import pl.pawelcz.campaignHub.campaign.entity.Keyword;
import pl.pawelcz.campaignHub.campaign.entity.Town;
import pl.pawelcz.campaignHub.campaign.exception.BusinessValidationException;
import pl.pawelcz.campaignHub.campaign.exception.InsufficientFundsException;
import pl.pawelcz.campaignHub.core.NotFoundException;
import pl.pawelcz.campaignHub.campaign.repository.CampaignRepository;
import pl.pawelcz.campaignHub.campaign.repository.EmeraldAccountRepository;
import pl.pawelcz.campaignHub.campaign.repository.KeywordRepository;
import pl.pawelcz.campaignHub.campaign.repository.TownRepository;

@Service
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final TownRepository townRepository;
    private final KeywordRepository keywordRepository;
    private final EmeraldAccountRepository emeraldAccountRepository;

    public CampaignServiceImpl(
        CampaignRepository campaignRepository,
        TownRepository townRepository,
        KeywordRepository keywordRepository,
        EmeraldAccountRepository emeraldAccountRepository
    ) {
        this.campaignRepository = campaignRepository;
        this.townRepository = townRepository;
        this.keywordRepository = keywordRepository;
        this.emeraldAccountRepository = emeraldAccountRepository;
    }

    @Override
    public List<CampaignResponse> getAllCampaigns() {
        return campaignRepository.findAll().stream().map(CampaignResponse::fromEntity).toList();
    }

    @Override
    public CampaignResponse getCampaignById(UUID id) {
        Campaign campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Campaign with id " + id + " not found"));
        return CampaignResponse.fromEntity(campaign);
    }

    @Transactional
    @Override
    public CampaignWithBalanceResponse createCampaign(CampaignRequest request) {
        validateRequest(request);

        EmeraldAccount account = requireAccount();
        ensureSufficientFunds(account, request.campaignFund());

        Campaign campaign = Campaign.builder()
            .name(request.name().trim())
            .keywords(resolveKeywords(request.keywords()))
            .bidAmount(request.bidAmount())
            .campaignFund(request.campaignFund())
            .status(request.status())
            .town(request.town() == null || request.town().isBlank() ? null : request.town().trim())
            .radiusInKm(request.radiusInKm())
            .build();
        Campaign saved = campaignRepository.save(campaign);

        account.setBalance(account.getBalance().subtract(request.campaignFund()));

        return new CampaignWithBalanceResponse(CampaignResponse.fromEntity(saved), account.getBalance());
    }

    @Transactional
    @Override
    public CampaignWithBalanceResponse updateCampaign(UUID id, CampaignRequest request) {
        validateRequest(request);

        Campaign existing = campaignRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Campaign with id " + id + " not found"));

        EmeraldAccount account = requireAccount();

        BigDecimal currentFund = existing.getCampaignFund();
        BigDecimal newFund = request.campaignFund();
        BigDecimal delta = newFund.subtract(currentFund);

        if (delta.signum() > 0) {
            ensureSufficientFunds(account, delta);
            account.setBalance(account.getBalance().subtract(delta));
        } else if (delta.signum() < 0) {
            account.setBalance(account.getBalance().add(delta.abs()));
        }

        applyRequest(existing, request);
        Campaign saved = campaignRepository.save(existing);

        return new CampaignWithBalanceResponse(CampaignResponse.fromEntity(saved), account.getBalance());
    }

    @Transactional
    @Override
    public void deleteCampaign(UUID id) {
        Campaign existing = campaignRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Campaign with id " + id + " not found"));

        EmeraldAccount account = requireAccount();
        account.setBalance(account.getBalance().add(existing.getCampaignFund()));

        campaignRepository.delete(existing);
    }

    @Override
    public List<String> searchKeywords(String query) {
        if (query == null || query.isBlank()) {
            return keywordRepository.findAll().stream().map(Keyword::getValue).sorted().toList();
        }
        return keywordRepository.findByValueContainingIgnoreCase(query).stream()
            .map(Keyword::getValue)
            .sorted()
            .toList();
    }

    @Override
    public List<String> getTowns() {
        return townRepository.findAll().stream().map(Town::getName).sorted().toList();
    }

    @Override
    public BigDecimal getEmeraldBalance() {
        return requireAccount().getBalance();
    }

    private void applyRequest(Campaign campaign, CampaignRequest request) {
        campaign.setName(request.name().trim());
        campaign.setKeywords(resolveKeywords(request.keywords()));
        campaign.setBidAmount(request.bidAmount());
        campaign.setCampaignFund(request.campaignFund());
        campaign.setStatus(request.status());
        campaign.setTown(request.town() == null || request.town().isBlank() ? null : request.town().trim());
        campaign.setRadiusInKm(request.radiusInKm());
    }

    private void validateRequest(CampaignRequest request) {
        if (request.town() != null && !request.town().isBlank()) {
            boolean townExists = townRepository.findByNameIgnoreCase(request.town().trim()).isPresent();
            if (!townExists) {
                throw new BusinessValidationException("Town is not in the available pre-populated list");
            }
        }
    }

    private Set<Keyword> resolveKeywords(Set<String> requestedKeywords) {
        return requestedKeywords.stream()
            .map(String::trim)
            .filter(keyword -> !keyword.isBlank())
            .map(keyword -> keywordRepository.findByValueIgnoreCase(keyword)
                .orElseThrow(() -> new BusinessValidationException("All campaign keywords must come from pre-populated typeahead values")))
            .collect(java.util.stream.Collectors.toSet());
    }

    private void ensureSufficientFunds(EmeraldAccount account, BigDecimal amountToDeduct) {
        if (account.getBalance().compareTo(amountToDeduct) < 0) {
            throw new InsufficientFundsException("Insufficient Emerald funds. Current balance: " + account.getBalance());
        }
    }

    private EmeraldAccount requireAccount() {
        EmeraldAccount account = emeraldAccountRepository.findTopByOrderByIdAsc();
        if (account == null) {
            throw new NotFoundException("Emerald account not configured");
        }
        return account;
    }

}
