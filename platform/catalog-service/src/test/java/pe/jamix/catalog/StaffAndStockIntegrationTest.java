package pe.jamix.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

/**
 * Team accounts, roles and the stock ledger. Uses products 8-11 and its own users so it never
 * interferes with CatalogApiIntegrationTest (same Spring context and in-memory database).
 */
@SpringBootTest(properties = {
        "JAMIX_ADMIN_PASSWORD=admin123",
        "JAMIX_CUSTOMER_PASSWORD=cliente123",
        "JAMIX_JWT_SECRET=test-only-secret-at-least-32-bytes-long-0000" })
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class StaffAndStockIntegrationTest {

    private static final String TEMP = "Temporal-123";
    private static final String OWN = "MiClaveSegura-2026";

    @Autowired
    MockMvc mvc;

    @Test
    void newWorkerMustReplaceTheTemporaryPasswordBeforeUsingThePanel() throws Exception {
        String owner = login("admin", "admin123");
        createUser(owner, "vendedora.uno", "Vendedora Uno", "SELLER").andExpect(status().isCreated());

        String body = loginResponse("vendedora.uno", TEMP);
        assertThat((Boolean) JsonPath.read(body, "$.mustChangePassword")).isTrue();
        String temporary = JsonPath.read(body, "$.accessToken");

        call(get("/api/v1/admin/products"), temporary).andExpect(status().isForbidden());
        call(get("/api/v1/me"), temporary).andExpect(status().isOk());

        String session = call(put("/api/v1/me/password"), temporary,
                "{\"currentPassword\":\"" + TEMP + "\",\"newPassword\":\"" + OWN + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andReturn().getResponse().getContentAsString();
        String fresh = JsonPath.read(session, "$.accessToken");

        call(get("/api/v1/me"), temporary).andExpect(status().isUnauthorized()); // old session closed
        call(get("/api/v1/admin/products"), fresh).andExpect(status().isOk());
    }

    @Test
    void sellerRegistersSalesButNotDeliveriesCountsOrTeamChanges() throws Exception {
        String seller = readyUser("vendedora.dos", "SELLER");

        // Product 8 (Top Naty) starts with 10.
        call(post("/api/v1/admin/products/8/movements"), seller, "{\"type\":\"VENTA_TIENDA\",\"quantity\":2,\"note\":\"Cliente en tienda\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.stock").value(8))
                .andExpect(jsonPath("$.movement.quantity").value(-2))
                .andExpect(jsonPath("$.movement.stockAfter").value(8))
                .andExpect(jsonPath("$.movement.username").value("vendedora.dos"));

        call(post("/api/v1/admin/products/8/movements"), seller, "{\"type\":\"INGRESO\",\"quantity\":5}")
                .andExpect(status().isForbidden());
        call(patch("/api/v1/admin/products/8/stock"), seller, "{\"stock\":50,\"version\":1}")
                .andExpect(status().isForbidden());
        call(get("/api/v1/admin/users"), seller).andExpect(status().isForbidden());

        call(get("/api/v1/admin/products/8/movements"), seller)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("VENTA_TIENDA"))
                .andExpect(jsonPath("$[0].note").value("Cliente en tienda"));

        String types = call(get("/api/v1/admin/movement-types"), seller).andReturn().getResponse().getContentAsString();
        List<String> allowed = JsonPath.read(types, "$[?(@.allowed == true)].type");
        assertThat(allowed).containsExactlyInAnyOrder("VENTA_TIENDA", "VENTA_WHATSAPP");
    }

    @Test
    void managerReceivesStockCountsItAndCannotGoBelowZero() throws Exception {
        String manager = readyUser("encargada.uno", "MANAGER");

        // Product 9 (Top Maricielo) starts with 10.
        call(post("/api/v1/admin/products/9/movements"), manager, "{\"type\":\"INGRESO\",\"quantity\":5}")
                .andExpect(jsonPath("$.product.stock").value(15));
        call(post("/api/v1/admin/products/9/movements"), manager, "{\"type\":\"MERMA\",\"quantity\":1,\"note\":\"Mancha\"}")
                .andExpect(jsonPath("$.product.stock").value(14));
        call(post("/api/v1/admin/products/9/movements"), manager, "{\"type\":\"VENTA_TIENDA\",\"quantity\":99}")
                .andExpect(status().isConflict());

        List<Number> versions = JsonPath.read(
                call(get("/api/v1/admin/products"), manager).andReturn().getResponse().getContentAsString(),
                "$[?(@.id == 9)].version");
        long version = versions.get(0).longValue();
        call(patch("/api/v1/admin/products/9/stock"), manager, "{\"stock\":12,\"version\":" + version + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(12));

        call(get("/api/v1/admin/products/9/movements"), manager)
                .andExpect(jsonPath("$[0].type").value("AJUSTE"))
                .andExpect(jsonPath("$[0].quantity").value(-2))
                .andExpect(jsonPath("$[0].note").value("Conteo físico"))
                .andExpect(jsonPath("$.length()").value(3));
        call(get("/api/v1/admin/users"), manager).andExpect(status().isForbidden());
    }

    @Test
    void deactivatingOrResettingAWorkerClosesTheirSessionImmediately() throws Exception {
        String owner = login("admin", "admin123");
        String seller = readyUser("vendedora.tres", "SELLER");
        long id = userId(owner, "vendedora.tres");

        call(post("/api/v1/admin/users/" + id + "/reset-password"), owner, "{\"temporaryPassword\":\"Otra-Temporal-1\"}")
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        call(get("/api/v1/me"), seller).andExpect(status().isUnauthorized());

        call(patch("/api/v1/admin/users/" + id), owner, "{\"active\":false}")
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"vendedora.tres\",\"password\":\"Otra-Temporal-1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theShopNeverLosesItsLastOwner() throws Exception {
        String owner = login("admin", "admin123");
        long ownerId = userId(owner, "admin");
        call(patch("/api/v1/admin/users/" + ownerId), owner, "{\"active\":false}").andExpect(status().isConflict());
        call(patch("/api/v1/admin/users/" + ownerId), owner, "{\"role\":\"SELLER\"}").andExpect(status().isConflict());
    }

    @Test
    void accountRulesAreValidated() throws Exception {
        String owner = login("admin", "admin123");
        call(post("/api/v1/admin/users"), owner,
                "{\"username\":\"corta\",\"displayName\":\"X\",\"role\":\"SELLER\",\"temporaryPassword\":\"123\"}")
                .andExpect(status().isBadRequest());
        createUser(owner, "repetida", "Repetida", "SELLER").andExpect(status().isCreated());
        createUser(owner, "Repetida", "Otra", "SELLER").andExpect(status().isConflict()); // usernames are case-insensitive
    }

    @Test
    void webOrdersAreRecordedInTheLedger() throws Exception {
        String customer = login("cliente", "cliente123");
        String order = call(post("/api/v1/orders"), customer, "{\"items\":[{\"productId\":10,\"quantity\":1}]}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int orderId = JsonPath.read(order, "$.id");

        String owner = login("admin", "admin123");
        call(get("/api/v1/admin/products/10/movements"), owner)
                .andExpect(jsonPath("$[0].type").value("VENTA_WEB"))
                .andExpect(jsonPath("$[0].username").value("cliente"))
                .andExpect(jsonPath("$[0].note").value("Pedido web #" + orderId));
        call(get("/api/v1/admin/movements"), owner)
                .andExpect(jsonPath("$[*].note", hasItem("Pedido web #" + orderId)));
    }

    // ---------------------------------------------------------------- helpers

    /** Creates a staff account and completes the first-login password change; returns a usable token. */
    private String readyUser(String username, String role) throws Exception {
        String owner = login("admin", "admin123");
        createUser(owner, username, username, role).andExpect(status().isCreated());
        String temporary = login(username, TEMP);
        String session = call(put("/api/v1/me/password"), temporary,
                "{\"currentPassword\":\"" + TEMP + "\",\"newPassword\":\"" + OWN + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(session, "$.accessToken");
    }

    private ResultActions createUser(String owner, String username, String name, String role) throws Exception {
        return call(post("/api/v1/admin/users"), owner, "{\"username\":\"" + username + "\",\"displayName\":\"" + name
                + "\",\"role\":\"" + role + "\",\"temporaryPassword\":\"" + TEMP + "\"}");
    }

    private long userId(String owner, String username) throws Exception {
        String team = call(get("/api/v1/admin/users"), owner).andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(team, "$[?(@.username == '" + username + "')].id");
        return ids.get(0).longValue();
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String token, String json) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String loginResponse(String username, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String login(String username, String password) throws Exception {
        return JsonPath.read(loginResponse(username, password), "$.accessToken");
    }
}
