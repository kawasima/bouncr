package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.api.resource.MockFactory;
import net.unit8.bouncr.data.OidcApplication;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OidcApplicationRepositoryTest {
    private DSLContext dsl;
    private OidcApplicationRepository repo;

    @BeforeEach
    void setup() {
        dsl = MockFactory.beginTransaction();
        repo = new OidcApplicationRepository(dsl);
    }

    @AfterEach
    void tearDown() {
        MockFactory.rollback();
    }

    @Test
    void insertAndFindByName_withLogoutUris() {
        repo.insert(
                "oidc-app-a",
                "client-a",
                "secret-a",
                new byte[]{1, 2, 3},
                new byte[]{4, 5, 6},
                "https://client-a.example",
                "https://client-a.example/callback",
                "desc-a",
                "https://client-a.example/backchannel-logout",
                "https://client-a.example/frontchannel-logout"
        );

        Optional<OidcApplication> found = repo.findByName("oidc-app-a");
        assertThat(found).isPresent();
        assertThat(found.get().backchannelLogoutUri()).isNotNull();
        assertThat(found.get().backchannelLogoutUri().toString())
                .isEqualTo("https://client-a.example/backchannel-logout");
        assertThat(found.get().frontchannelLogoutUri()).isNotNull();
        assertThat(found.get().frontchannelLogoutUri().toString())
                .isEqualTo("https://client-a.example/frontchannel-logout");
    }

    @Test
    void update_overwritesLogoutUris() {
        repo.insert(
                "oidc-app-b",
                "client-b",
                "secret-b",
                new byte[]{1},
                new byte[]{2},
                "https://client-b.example",
                "https://client-b.example/callback",
                "desc-b",
                "https://client-b.example/backchannel-logout",
                "https://client-b.example/frontchannel-logout"
        );

        repo.update(
                "oidc-app-b",
                "oidc-app-b",
                null,
                null,
                null,
                null,
                "https://client-b.example/home-new",
                "https://client-b.example/callback-new",
                "desc-b-new",
                "https://client-b.example/backchannel-logout-new",
                "https://client-b.example/frontchannel-logout-new"
        );

        OidcApplication updated = repo.findByName("oidc-app-b").orElseThrow();
        assertThat(updated.homeUrl().toString()).isEqualTo("https://client-b.example/home-new");
        assertThat(updated.callbackUrl().toString()).isEqualTo("https://client-b.example/callback-new");
        assertThat(updated.backchannelLogoutUri().toString())
                .isEqualTo("https://client-b.example/backchannel-logout-new");
        assertThat(updated.frontchannelLogoutUri().toString())
                .isEqualTo("https://client-b.example/frontchannel-logout-new");
    }
}
