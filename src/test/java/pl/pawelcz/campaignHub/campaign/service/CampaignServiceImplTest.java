package pl.pawelcz.campaignHub.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pawelcz.campaignHub.campaign.dto.CampaignRequest;
import pl.pawelcz.campaignHub.campaign.dto.CampaignWithBalanceResponse;
import pl.pawelcz.campaignHub.campaign.entity.Campaign;
import pl.pawelcz.campaignHub.campaign.entity.CampaignStatus;
import pl.pawelcz.campaignHub.campaign.entity.EmeraldAccount;
import pl.pawelcz.campaignHub.campaign.entity.Keyword;
import pl.pawelcz.campaignHub.campaign.entity.Town;
import pl.pawelcz.campaignHub.campaign.exception.BusinessValidationException;
import pl.pawelcz.campaignHub.campaign.exception.InsufficientFundsException;
import pl.pawelcz.campaignHub.campaign.repository.CampaignRepository;
import pl.pawelcz.campaignHub.campaign.repository.EmeraldAccountRepository;
import pl.pawelcz.campaignHub.campaign.repository.KeywordRepository;
import pl.pawelcz.campaignHub.campaign.repository.TownRepository;
import pl.pawelcz.campaignHub.core.NotFoundException;
import pl.pawelcz.campaignHub.product.entity.Product;
import pl.pawelcz.campaignHub.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CampaignServiceImplTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private TownRepository townRepository;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private EmeraldAccountRepository emeraldAccountRepository;

    @Mock
    private ProductRepository productRepository;

    private CampaignServiceImpl campaignService;

    @BeforeEach
    void setUp() {
        campaignService = new CampaignServiceImpl(campaignRepository, townRepository, keywordRepository, emeraldAccountRepository, productRepository);
    }

    @Test
    void shouldCreateCampaignAndDecreaseEmeraldBalance() {
        Keyword books = Keyword.builder().value("books").build();
        Keyword electronics = Keyword.builder().value("electronics").build();
        EmeraldAccount account = EmeraldAccount.builder().balance(new BigDecimal("1000.00")).build();
        Product product = Product.builder().id(PRODUCT_ID).name("Laptop Pro 15").build();
        Campaign savedCampaign = Campaign.builder()
            .id(UUID.randomUUID())
            .product(product)
            .name("School Promo")
            .keywords(Set.of(books, electronics))
            .bidAmount(new BigDecimal("2.50"))
            .campaignFund(new BigDecimal("200.00"))
            .status(CampaignStatus.ON)
            .town("Warsaw")
            .radiusInKm(20)
            .build();

        when(townRepository.findByNameIgnoreCase("Warsaw")).thenReturn(Optional.of(Town.builder().name("Warsaw").build()));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(keywordRepository.findByValueIgnoreCase("books")).thenReturn(Optional.of(books));
        when(keywordRepository.findByValueIgnoreCase("electronics")).thenReturn(Optional.of(electronics));
        when(emeraldAccountRepository.findTopByOrderByIdAsc()).thenReturn(account);
        when(campaignRepository.existsByProductIdAndStatus(PRODUCT_ID, CampaignStatus.ON)).thenReturn(false);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);

        CampaignWithBalanceResponse result = campaignService.createCampaign(buildRequest());

        assertThat(result.campaign()).isNotNull();
        assertThat(result.campaign().name()).isEqualTo("School Promo");
        assertThat(result.emeraldBalance()).isEqualByComparingTo("800.00");
        verify(campaignRepository).save(any(Campaign.class));
    }

    @Test
    void shouldThrowWhenTownIsNotOnList() {
        when(townRepository.findByNameIgnoreCase("Warsaw")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.createCampaign(buildRequest()))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("Town is not in the available pre-populated list");
    }

    @Test
    void shouldThrowWhenKeywordIsNotOnList() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(Product.builder().id(PRODUCT_ID).name("Laptop Pro 15").build()));
        when(townRepository.findByNameIgnoreCase("Warsaw")).thenReturn(Optional.of(Town.builder().name("Warsaw").build()));
        when(keywordRepository.findByValueIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(emeraldAccountRepository.findTopByOrderByIdAsc()).thenReturn(EmeraldAccount.builder().balance(new BigDecimal("1000.00")).build());
        when(campaignRepository.existsByProductIdAndStatus(PRODUCT_ID, CampaignStatus.ON)).thenReturn(false);

        assertThatThrownBy(() -> campaignService.createCampaign(buildRequest()))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("All campaign keywords must come from pre-populated typeahead values");
    }

    @Test
    void shouldThrowWhenInsufficientFunds() {
        EmeraldAccount account = EmeraldAccount.builder().balance(new BigDecimal("100.00")).build();

        when(townRepository.findByNameIgnoreCase("Warsaw")).thenReturn(Optional.of(Town.builder().name("Warsaw").build()));
        when(emeraldAccountRepository.findTopByOrderByIdAsc()).thenReturn(account);

        assertThatThrownBy(() -> campaignService.createCampaign(buildRequest()))
            .isInstanceOf(InsufficientFundsException.class)
            .hasMessageContaining("Insufficient Emerald funds");
    }

    @Test
    void shouldDeleteCampaignAndRefundBalance() {
        UUID campaignId = UUID.randomUUID();
        Campaign existing = Campaign.builder()
            .id(campaignId)
            .product(Product.builder().id(UUID.randomUUID()).name("Laptop Pro 15").build())
            .name("Old Campaign")
            .campaignFund(new BigDecimal("300.00"))
            .bidAmount(new BigDecimal("1.00"))
            .keywords(Set.of())
            .status(CampaignStatus.ON)
            .radiusInKm(10)
            .build();
        EmeraldAccount account = EmeraldAccount.builder().balance(new BigDecimal("500.00")).build();

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(existing));
        when(emeraldAccountRepository.findTopByOrderByIdAsc()).thenReturn(account);

        campaignService.deleteCampaign(campaignId);

        assertThat(account.getBalance()).isEqualByComparingTo("800.00");
        verify(campaignRepository).delete(existing);
    }

    @Test
    void shouldThrowWhenCampaignNotFound() {
        UUID campaignId = UUID.randomUUID();
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.getCampaignById(campaignId))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Campaign with id");
    }

    @Test
    void shouldThrowWhenAnotherActiveCampaignExistsForProductOnCreate() {
        when(townRepository.findByNameIgnoreCase("Warsaw")).thenReturn(Optional.of(Town.builder().name("Warsaw").build()));
        when(emeraldAccountRepository.findTopByOrderByIdAsc()).thenReturn(EmeraldAccount.builder().balance(new BigDecimal("1000.00")).build());
        when(campaignRepository.existsByProductIdAndStatus(PRODUCT_ID, CampaignStatus.ON)).thenReturn(true);

        assertThatThrownBy(() -> campaignService.createCampaign(buildRequest()))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("Product can have max one active campaign");
    }

    @Test
    void shouldThrowWhenAnotherActiveCampaignExistsForProductOnUpdate() {
        UUID campaignId = UUID.randomUUID();
        Campaign existing = Campaign.builder()
            .id(campaignId)
            .product(Product.builder().id(PRODUCT_ID).name("Laptop Pro 15").build())
            .name("Existing")
            .campaignFund(new BigDecimal("100.00"))
            .bidAmount(new BigDecimal("1.00"))
            .keywords(Set.of())
            .status(CampaignStatus.OFF)
            .radiusInKm(10)
            .build();

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(existing));
        when(townRepository.findByNameIgnoreCase("Warsaw")).thenReturn(Optional.of(Town.builder().name("Warsaw").build()));
        when(emeraldAccountRepository.findTopByOrderByIdAsc()).thenReturn(EmeraldAccount.builder().balance(new BigDecimal("1000.00")).build());
        when(campaignRepository.existsByProductIdAndStatusAndIdNot(eq(PRODUCT_ID), eq(CampaignStatus.ON), eq(campaignId))).thenReturn(true);

        assertThatThrownBy(() -> campaignService.updateCampaign(campaignId, buildRequest()))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("Product can have max one active campaign");
    }

    @Test
    void shouldSearchKeywords() {
        when(keywordRepository.findByValueContainingIgnoreCase("book")).thenReturn(List.of(
            Keyword.builder().value("books").build(),
            Keyword.builder().value("ebooks").build()
        ));

        List<String> result = campaignService.searchKeywords("book");

        assertThat(result).containsExactly("books", "ebooks");
    }

    @Test
    void shouldReturnAllKeywordsWhenQueryBlank() {
        when(keywordRepository.findAll()).thenReturn(List.of(
            Keyword.builder().value("sports").build(),
            Keyword.builder().value("books").build()
        ));

        List<String> result = campaignService.searchKeywords(" ");

        assertThat(result).containsExactly("books", "sports");
    }

    @Test
    void shouldReturnSortedTowns() {
        when(townRepository.findAll()).thenReturn(List.of(
            Town.builder().name("Warsaw").build(),
            Town.builder().name("Krakow").build()
        ));

        List<String> result = campaignService.getTowns();

        assertThat(result).containsExactly("Krakow", "Warsaw");
    }

    @Test
    void shouldThrowWhenEmeraldAccountMissing() {
        when(emeraldAccountRepository.findTopByOrderByIdAsc()).thenReturn(null);

        assertThatThrownBy(() -> campaignService.getEmeraldBalance())
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Emerald account not configured");
    }

    private CampaignRequest buildRequest() {
        return new CampaignRequest(
            PRODUCT_ID,
            "School Promo",
            Set.of("books", "electronics"),
            new BigDecimal("2.50"),
            new BigDecimal("200.00"),
            CampaignStatus.ON,
            "Warsaw",
            20
        );
    }
}
