package pl.pawelcz.campaignHub.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.pawelcz.campaignHub.product.repository.ProductRepository;
import pl.pawelcz.campaignHub.seller.auth.security.SellerPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
class CampaignControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();
    private String testProductId;
    private String secondProductId;
    private String sellerId;

    @BeforeEach
    void setUp() {
        var firstProduct = productRepository.findByNameIgnoreCase("Laptop Pro 15").orElseThrow();
        var otherProduct = productRepository.findByNameIgnoreCase("City Bike X").orElseThrow();
        testProductId = firstProduct.getId().toString();
        secondProductId = otherProduct.getId().toString();
        sellerId = firstProduct.getSeller().getId().toString();
    }

    @Test
    void shouldCreateCampaignAndDeductBalance() throws Exception {
        BigDecimal before = readBalance();

        MvcResult createResult = mockMvc.perform(post("/api/campaigns")
            .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCampaignPayload()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.campaign.id").isNotEmpty())
            .andExpect(jsonPath("$.campaign.name").value("Back to School 2026"))
            .andExpect(jsonPath("$.campaign.status").value("ON"))
            .andReturn();

        Map<String, Object> createJson = jsonParser.parseMap(createResult.getResponse().getContentAsString());
        BigDecimal responseBalance = new BigDecimal(createJson.get("emeraldBalance").toString());
        BigDecimal after = readBalance();

        assertThat(responseBalance).isEqualByComparingTo(after);
        assertThat(before.subtract(after)).isEqualByComparingTo("1500.00");
    }

    @Test
    void shouldReturnDedicatedErrorForInvalidStatus() throws Exception {
        mockMvc.perform(post("/api/campaigns")
            .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidStatusPayload()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.detail").value("Invalid status value. Allowed values: ON, OFF"))
            .andExpect(jsonPath("$.errors[0]").value("status: must be ON or OFF"));
    }

    @Test
    void shouldDeleteCampaignAndReturnNoContent() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/campaigns")
            .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCampaignPayloadFor(secondProductId)))
            .andExpect(status().isCreated())
            .andReturn();

        Map<String, Object> createJson = jsonParser.parseMap(createResult.getResponse().getContentAsString());
        @SuppressWarnings("unchecked")
        Map<String, Object> campaign = (Map<String, Object>) createJson.get("campaign");
        String id = campaign.get("id").toString();

        mockMvc.perform(delete("/api/campaigns/{id}", id).with(authentication(auth())))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/campaigns/{id}", id).with(authentication(auth())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Campaign with id " + id + " not found"));
    }

    private BigDecimal readBalance() throws Exception {
        MvcResult balanceResult = mockMvc.perform(get("/api/emerald-account/balance").with(authentication(auth())))
            .andExpect(status().isOk())
            .andReturn();
        Map<String, Object> balanceJson = jsonParser.parseMap(balanceResult.getResponse().getContentAsString());
        return new BigDecimal(balanceJson.get("balance").toString());
    }

    private String validCampaignPayload() {
        return validCampaignPayloadFor(testProductId);
    }

    private String validCampaignPayloadFor(String productId) {
        return "{" +
            "\"productId\":\"" + productId + "\"," +
            "\"name\":\"Back to School 2026\"," +
            "\"keywords\":[\"books\",\"electronics\"]," +
            "\"bidAmount\":2.50," +
            "\"campaignFund\":1500.00," +
            "\"status\":\"ON\"," +
            "\"town\":\"Warsaw\"," +
            "\"radiusInKm\":25" +
            "}";
    }

    private String invalidStatusPayload() {
        return "{" +
            "\"productId\":\"" + testProductId + "\"," +
            "\"name\":\"Back to School 2026\"," +
            "\"keywords\":[\"books\",\"electronics\"]," +
            "\"bidAmount\":2.50," +
            "\"campaignFund\":1500.00," +
            "\"status\":\"OssdNs\"," +
            "\"town\":\"Warsaw\"," +
            "\"radiusInKm\":25" +
            "}";
    }

    private UsernamePasswordAuthenticationToken auth() {
        SellerPrincipal principal = new SellerPrincipal(java.util.UUID.fromString(sellerId), "seller.one@campaignhub.local");
        return new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of());
    }
}
