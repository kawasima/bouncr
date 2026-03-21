package net.unit8.bouncr.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserManagementE2ETest extends E2ETestBase {
    private APIRequestContext adminApi;

    @BeforeAll
    void setupApi() {
        adminApi = adminContext();
    }

    @AfterAll
    void cleanupApi() {
        if (adminApi != null) {
            adminApi.dispose();
        }
    }

    @Test
    @Tag("e2e-smoke")
    void userCrud_createReadUpdateDelete() throws Exception {
        String account = "e2e_user_" + suffix();

        APIResponse create = postJson(adminApi, "/bouncr/api/users", Map.of(
                "account", account,
                "email", account + "@example.com",
                "name", "E2E User"));
        assertThat(create.status()).isEqualTo(201);

        APIResponse getCreated = adminApi.get("/bouncr/api/user/" + account);
        assertThat(getCreated.status()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> user = JSON.readValue(getCreated.body(), Map.class);
        assertThat(user.get("account")).isEqualTo(account);

        APIResponse update = putJson(adminApi, "/bouncr/api/user/" + account, Map.of(
                "email", account + "+updated@example.com",
                "name", "E2E User Updated"));
        assertThat(update.status()).isEqualTo(201);

        APIResponse delete = adminApi.delete("/bouncr/api/user/" + account);
        assertThat(delete.status()).isIn(200, 204);

        APIResponse notFound = adminApi.get("/bouncr/api/user/" + account);
        assertThat(notFound.status()).isEqualTo(404);
    }

    @Test
    @Tag("e2e-full")
    void users_duplicateAccount_returns409() throws Exception {
        String account = "dup_user_" + suffix();
        Map<String, Object> payload = Map.of("account", account, "email", account + "@example.com", "name", "Dup");

        APIResponse first = postJson(adminApi, "/bouncr/api/users", payload);
        assertThat(first.status()).isEqualTo(201);

        APIResponse duplicate = postJson(adminApi, "/bouncr/api/users", payload);
        assertThat(duplicate.status()).isEqualTo(409);
    }

    @Test
    @Tag("e2e-full")
    void users_missingAccount_returns400() throws Exception {
        APIResponse response = postJson(adminApi, "/bouncr/api/users", Map.of(
                "email", "missing-account@example.com",
                "name", "Missing Account"));
        assertThat(response.status()).isEqualTo(400);
    }

    @Test
    @Tag("e2e-full")
    void users_withoutCredential_returns401() {
        APIResponse response = api.get("/bouncr/api/users");
        assertThat(response.status()).isEqualTo(401);
    }

    @Test
    @Tag("e2e-full")
    void users_listContainsCreatedUser() throws Exception {
        String account = "list_user_" + suffix();
        APIResponse create = postJson(adminApi, "/bouncr/api/users", Map.of(
                "account", account,
                "email", account + "@example.com",
                "name", "List Target"));
        assertThat(create.status()).isEqualTo(201);

        APIResponse list = adminApi.get("/bouncr/api/users?q=" + account + "&limit=5");
        assertThat(list.status()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = JSON.readValue(list.body(), List.class);
        assertThat(users.stream().map(u -> (String) u.get("account"))).contains(account);
    }

    private long suffix() {
        return System.nanoTime();
    }
}
