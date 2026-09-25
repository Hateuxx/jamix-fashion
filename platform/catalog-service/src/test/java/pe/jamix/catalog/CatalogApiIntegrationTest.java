package pe.jamix.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Runs against H2 in MySQL mode: Flyway applies the real migrations and Hibernate validates the
 * entities against them (ddl-auto=validate), so a broken migration or mapping fails here first.
 */
// Test credentials outrank the developer's .env and OS variables, so results don't depend on who runs them.
@SpringBootTest(properties = {
        "JAMIX_ADMIN_PASSWORD=admin123",
        "JAMIX_CUSTOMER_PASSWORD=cliente123",
        "JAMIX_JWT_SECRET=test-only-secret-at-least-32-bytes-long-0000" })
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class CatalogApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void catalogIsPublicAndFilterable() throws Exception {
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(35)))
                .andExpect(jsonPath("$[0].slug").value("falda-bobo"))
                .andExpect(jsonPath("$[0].type").value("Faldas"))
                .andExpect(jsonPath("$[0].priceLabel").value("S/ 25"))
                .andExpect(jsonPath("$[0].wholesaleLabel").value("S/ 18"))
                .andExpect(jsonPath("$[0].stock").doesNotExist())
                // Grouped Ella -> Él -> Niños even though Niños products were seeded before most Ella ones.
                .andExpect(jsonPath("$[26].category").value("el"))
                .andExpect(jsonPath("$[34].category").value("ninos"));

        // V4 migration: gallery in display order.
        mvc.perform(get("/api/v1/products/falda-bobo"))
                .andExpect(jsonPath("$.gallery", hasSize(2)))
                .andExpect(jsonPath("$.gallery[0]").value("/img/falda-bobo-look-1.jpeg"));
        mvc.perform(get("/api/v1/products/top-naty"))
                .andExpect(jsonPath("$.gallery", hasSize(0)));

        // V3 migration: description matches the new photo.
        mvc.perform(get("/api/v1/products/vestido-rita"))
                .andExpect(jsonPath("$.description").value(containsString("mangas tres cuartos")))
                .andExpect(jsonPath("$.imageUrl").value("/img/vestido-rita.jpeg"));

        // Removed from the catalogue.
        mvc.perform(get("/api/v1/products/pantalon-bolsillo")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/products").param("category", "el"))
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/v1/products").param("category", "ninos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].priceLabel").value("S/ 50 – 60"))
                .andExpect(jsonPath("$[0].wholesaleLabel").value("S/ 32 – 36"));

        mvc.perform(get("/api/v1/products").param("category", "mascotas"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void everyProductPhotoIsServed() throws Exception {
        String body = mvc.perform(get("/api/v1/products")).andReturn().getResponse().getContentAsString();
        List<String> images = new java.util.ArrayList<>(JsonPath.read(body, "$[*].imageUrl"));
        List<String> gallery = JsonPath.read(body, "$[*].gallery[*]");
        images.addAll(gallery);
        assertThat(gallery).hasSize(5);
        for (String image : images) {
            if (image == null) {
                continue; // "Foto próximamente" placeholder
            }
            mvc.perform(get(image))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
        }
    }

    @Test
    void everyGarmentHasAGenderForTheFamilyLook() throws Exception {
        String body = mvc.perform(get("/api/v1/products")).andReturn().getResponse().getContentAsString();
        List<String> genders = JsonPath.read(body, "$[*].gender");
        assertThat(genders).hasSize(35).doesNotContain("unisex");

        mvc.perform(get("/api/v1/products/vestido-rita")).andExpect(jsonPath("$.gender").value("femenino"));
        mvc.perform(get("/api/v1/products/short-bolsillo")).andExpect(jsonPath("$.gender").value("masculino"));
        // "Niños" mixes both: girls' dresses and boys' sets.
        mvc.perform(get("/api/v1/products/vestido-tunica")).andExpect(jsonPath("$.gender").value("femenino"));
        mvc.perform(get("/api/v1/products/conjunto-walter")).andExpect(jsonPath("$.gender").value("masculino"));
    }

    @Test
    void ordersRequireAToken() throws Exception {
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void badCredentialsAreRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerOrderMergesLinesAndAppliesWholesaleFromSixUnits() throws Exception {
        String token = login("cliente", "cliente123");
        // Vestido Rita (id 2): 3 + 3 units in separate lines = 6 -> wholesale S/ 33 each.
        String created = mvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":2,\"quantity\":3},{\"productId\":2,\"quantity\":3}]}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].quantity").value(6))
                .andExpect(jsonPath("$.lines[0].wholesale").value(true))
                .andExpect(jsonPath("$.total").value(198))
                .andReturn().getResponse().getContentAsString();
        Integer orderId = JsonPath.read(created, "$.id");

        // Other test classes share this in-memory database and may have placed orders as "cliente" too.
        mvc.perform(get("/api/v1/orders/mine").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(orderId)));
    }

    @Test
    void insufficientStockRejectsTheWholeOrderAndRollsBack() throws Exception {
        String customer = login("cliente", "cliente123");
        // Falda Bobo (id 1) has stock; Vestido Túnica (id 4) has 15, so asking for 100 must fail.
        mvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":2},{\"productId\":4,\"quantity\":100}]}"))
                .andExpect(status().isConflict());

        String admin = login("admin", "admin123");
        mvc.perform(get("/api/v1/admin/products").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stock").value(24))
                .andExpect(jsonPath("$[3].stock").value(15));
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        String customer = login("cliente", "cliente123");
        mvc.perform(get("/api/v1/admin/products").header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void stockUpdateUsesOptimisticVersion() throws Exception {
        String admin = login("admin", "admin123");
        // Vestido Stephie (id 3) is only touched by this test.
        mvc.perform(patch("/api/v1/admin/products/3/stock").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stock\":7,\"version\":99}"))
                .andExpect(status().isConflict());

        mvc.perform(patch("/api/v1/admin/products/3/stock").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stock\":7,\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(7))
                .andExpect(jsonPath("$.version").value(1));

        mvc.perform(patch("/api/v1/admin/products/3/stock").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stock\":-1,\"version\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void storefrontIsServerRenderedWithStrictCsp() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(header().string("Content-Security-Policy", containsString("form-action 'self' http://localhost:4000")))
                .andExpect(content().string(containsString("Toda la familia de blanco")))
                .andExpect(content().string(containsString("Vestimos el recuerdo que va a quedar colgado en tu sala")))
                .andExpect(content().string(containsString("Vestido Rita")))
                .andExpect(content().string(containsString("/img/top-marleny.jpeg")))
                .andExpect(content().string(containsString("/img/opt/top-marleny-480.webp 480w")))
                .andExpect(content().string(containsString("/img/opt/hero-960.webp 960w")))
                .andExpect(content().string(containsString("Mayorista S/ 32 – 36 · desde 6 unid.")))
                .andExpect(content().string(containsString("\"@type\":\"ClothingStore\"")))
                .andExpect(content().string(containsString("href=\"/?categoria=ninos#catalogo\"")))
                .andExpect(content().string(containsString("S/ 16")));

        mvc.perform(get("/").param("categoria", "ninos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vestido Túnica")))
                .andExpect(content().string(not(containsString("Vestido Rita"))));
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
