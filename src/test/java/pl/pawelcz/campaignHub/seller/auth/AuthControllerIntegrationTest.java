package pl.pawelcz.campaignHub.seller.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonParser parser = JsonParserFactory.getJsonParser();

    @Test
    void shouldRegisterLoginAndAccessProtectedEndpoint() throws Exception {
        String email = "seller." + System.currentTimeMillis() + "@example.com";

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload(email, "StrongPass123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn();

        String accessToken = parser.parseMap(registerResult.getResponse().getContentAsString()).get("accessToken").toString();

        mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload(email, "StrongPass123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void shouldRefreshAndLogoutTokenFlow() throws Exception {
        String email = "seller." + System.nanoTime() + "@example.com";

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload(email, "StrongPass123")))
            .andExpect(status().isOk())
            .andReturn();

        String refreshToken = parser.parseMap(registerResult.getResponse().getContentAsString()).get("refreshToken").toString();

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectProtectedEndpointWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWeakPasswordOnRegister() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload("weak." + System.nanoTime() + "@example.com", "weakpass")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Password must include upper-case, lower-case and digit"));
    }

    private String registerPayload(String email, String password) {
        return "{" +
            "\"email\":\"" + email + "\"," +
            "\"displayName\":\"Seller Test\"," +
            "\"password\":\"" + password + "\"" +
            "}";
    }

    private String loginPayload(String email, String password) {
        return "{" +
            "\"email\":\"" + email + "\"," +
            "\"password\":\"" + password + "\"" +
            "}";
    }
}
