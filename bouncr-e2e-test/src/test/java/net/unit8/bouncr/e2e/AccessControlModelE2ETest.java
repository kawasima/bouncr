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

class AccessControlModelE2ETest extends E2ETestBase {
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
    void permissionRoleAndGroupMembership_roundTrip() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String permissionName = "e2e_perm:" + suffix;
        String roleName = "e2e_role_" + suffix;
        String groupName = "e2e_group_" + suffix;
        String account = "e2e_member_" + suffix;

        APIResponse createPermission = postJson(adminApi, "/bouncr/api/permissions", Map.of(
                "name", permissionName,
                "description", "E2E permission"));
        assertThat(createPermission.status()).isEqualTo(201);

        APIResponse createRole = postJson(adminApi, "/bouncr/api/roles", Map.of(
                "name", roleName,
                "description", "E2E role"));
        assertThat(createRole.status()).isEqualTo(201);

        APIResponse addRolePermission = postJson(adminApi,
                "/bouncr/api/role/" + roleName + "/permissions",
                List.of(permissionName));
        assertThat(addRolePermission.status()).isIn(200, 201);

        APIResponse rolePermissions = adminApi.get("/bouncr/api/role/" + roleName + "/permissions");
        assertThat(rolePermissions.status()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> permissions = JSON.readValue(rolePermissions.body(), List.class);
        assertThat(permissions.stream().map(p -> (String) p.get("name"))).contains(permissionName);

        APIResponse createGroup = postJson(adminApi, "/bouncr/api/groups", Map.of(
                "name", groupName,
                "description", "E2E group"));
        assertThat(createGroup.status()).isEqualTo(201);

        APIResponse createUser = postJson(adminApi, "/bouncr/api/users", Map.of(
                "account", account,
                "email", account + "@example.com",
                "name", "E2E Member"));
        assertThat(createUser.status()).isEqualTo(201);

        APIResponse addGroupUser = postJson(adminApi,
                "/bouncr/api/group/" + groupName + "/users",
                List.of(account));
        assertThat(addGroupUser.status()).isIn(200, 201);

        APIResponse groupUsers = adminApi.get("/bouncr/api/group/" + groupName + "/users");
        assertThat(groupUsers.status()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = JSON.readValue(groupUsers.body(), List.class);
        assertThat(users.stream().map(u -> (String) u.get("account"))).contains(account);
    }

    @Test
    @Tag("e2e-full")
    void permissions_duplicateName_returns409() throws Exception {
        String permissionName = "dup_perm:" + System.nanoTime();
        Map<String, Object> payload = Map.of(
                "name", permissionName,
                "description", "Duplicate permission");

        APIResponse first = postJson(adminApi, "/bouncr/api/permissions", payload);
        assertThat(first.status()).isEqualTo(201);

        APIResponse duplicate = postJson(adminApi, "/bouncr/api/permissions", payload);
        assertThat(duplicate.status()).isEqualTo(409);
    }

    @Test
    @Tag("e2e-full")
    void rolePermissions_unknownRole_returns404() throws Exception {
        APIResponse response = adminApi.get("/bouncr/api/role/not_found_role/permissions");
        assertThat(response.status()).isEqualTo(404);
    }

    @Test
    @Tag("e2e-full")
    void groupUsers_malformedBody_returns400() throws Exception {
        String groupName = "malformed_group_" + System.nanoTime();
        APIResponse createGroup = postJson(adminApi, "/bouncr/api/groups", Map.of(
                "name", groupName,
                "description", "Malformed target group"));
        assertThat(createGroup.status()).isEqualTo(201);

        APIResponse malformed = postJson(adminApi,
                "/bouncr/api/group/" + groupName + "/users",
                Map.of("account", "someone"));
        assertThat(malformed.status()).isEqualTo(400);
    }

    @Test
    @Tag("e2e-full")
    void groups_withoutCredential_returns401() {
        APIResponse response = api.get("/bouncr/api/groups");
        assertThat(response.status()).isEqualTo(401);
    }
}
