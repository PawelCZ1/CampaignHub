package pl.pawelcz.campaignHub.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.pawelcz.campaignHub.product.repository.ProductRepository;
import pl.pawelcz.campaignHub.seller.auth.security.SellerPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();
    private String sellerId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        sellerId = productRepository.findByNameIgnoreCase("Laptop Pro 15")
            .orElseThrow()
            .getSeller()
            .getId()
            .toString();
    }

    @Test
    void shouldCreateAndGetProduct() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/products")
            .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProductPayload()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.name").value("Gaming Monitor 27"))
            .andReturn();

        String id = jsonParser.parseMap(createResult.getResponse().getContentAsString()).get("id").toString();

        mockMvc.perform(get("/api/products/{id}", id).with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Gaming Monitor 27"));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/products")
            .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProductPayload()))
            .andExpect(status().isCreated())
            .andReturn();

        String id = jsonParser.parseMap(createResult.getResponse().getContentAsString()).get("id").toString();

        mockMvc.perform(put("/api/products/{id}", id)
            .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedProductPayload()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Gaming Monitor 32"))
            .andExpect(jsonPath("$.description").value("Monitor 32 cale 240Hz"));
    }

    @Test
    void shouldDeleteProductAndReturnNotFoundAfterwards() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/products")
            .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProductPayload()))
            .andExpect(status().isCreated())
            .andReturn();

        String id = jsonParser.parseMap(createResult.getResponse().getContentAsString()).get("id").toString();

        mockMvc.perform(delete("/api/products/{id}", id).with(authentication(auth())))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", id).with(authentication(auth())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Product with id " + id + " not found"));
    }

    @Test
    void shouldReturnValidationErrorWhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/products")
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"name\":\"   \"," +
                    "\"description\":\"x\"" +
                    "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.errors[0]").value("name: Product name is mandatory"));
    }

    @Test
    void shouldListProducts() throws Exception {
        mockMvc.perform(get("/api/products").with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNotEmpty());
    }

    private String validProductPayload() {
        return "{" +
            "\"name\":\"Gaming Monitor 27\"," +
            "\"description\":\"Monitor 27 cale 165Hz\"" +
            "}";
    }

    private String updatedProductPayload() {
        return "{" +
            "\"name\":\"Gaming Monitor 32\"," +
            "\"description\":\"Monitor 32 cale 240Hz\"" +
            "}";
    }

    private UsernamePasswordAuthenticationToken auth() {
        SellerPrincipal principal = new SellerPrincipal(java.util.UUID.fromString(sellerId), "seller.one@campaignhub.local");
        return new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of());
    }
}
