package pl.pawelcz.campaignHub.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    @Test
    void shouldCreateAndGetProduct() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProductPayload()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.name").value("Gaming Monitor 27"))
            .andReturn();

        String id = jsonParser.parseMap(createResult.getResponse().getContentAsString()).get("id").toString();

        mockMvc.perform(get("/api/products/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Gaming Monitor 27"));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProductPayload()))
            .andExpect(status().isCreated())
            .andReturn();

        String id = jsonParser.parseMap(createResult.getResponse().getContentAsString()).get("id").toString();

        mockMvc.perform(put("/api/products/{id}", id)
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProductPayload()))
            .andExpect(status().isCreated())
            .andReturn();

        String id = jsonParser.parseMap(createResult.getResponse().getContentAsString()).get("id").toString();

        mockMvc.perform(delete("/api/products/{id}", id))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Product with id " + id + " not found"));
    }

    @Test
    void shouldReturnValidationErrorWhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \",\"description\":\"x\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.errors[0]").value("name: Product name is mandatory"));
    }

    @Test
    void shouldListProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNotEmpty());
    }

    private String validProductPayload() {
        return """
            {
              "name": "Gaming Monitor 27",
              "description": "Monitor 27 cale 165Hz"
            }
            """;
    }

    private String updatedProductPayload() {
        return """
            {
              "name": "Gaming Monitor 32",
              "description": "Monitor 32 cale 240Hz"
            }
            """;
    }
}
