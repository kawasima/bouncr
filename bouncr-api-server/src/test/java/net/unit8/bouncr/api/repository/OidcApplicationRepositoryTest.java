package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.api.repository.OidcApplicationRepository.NullableUpdate;
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
        assertThat(found.get().metadata().backchannelLogoutUri()).isNotNull();
        assertThat(found.get().metadata().backchannelLogoutUri().toString())
                .isEqualTo("https://client-a.example/backchannel-logout");
        assertThat(found.get().metadata().frontchannelLogoutUri()).isNotNull();
        assertThat(found.get().metadata().frontchannelLogoutUri().toString())
                .isEqualTo("https://client-a.example/frontchannel-logout");
    }

    @Test
    void updateProfile_overwritesAllFields() {
        repo.insert("oidc-app-b", "client-b", "secret-b",
                new byte[]{1}, new byte[]{2},
                "https://client-b.example", "https://client-b.example/callback",
                "desc-b",
                "https://client-b.example/backchannel-logout",
                "https://client-b.example/frontchannel-logout");

        repo.updateProfile("oidc-app-b", "oidc-app-b",
                NullableUpdate.of("https://client-b.example/home-new"),
                NullableUpdate.of("https://client-b.example/callback-new"),
                NullableUpdate.of("desc-b-new"),
                NullableUpdate.of("https://client-b.example/backchannel-logout-new"),
                NullableUpdate.of("https://client-b.example/frontchannel-logout-new"));

        OidcApplication updated = repo.findByName("oidc-app-b").orElseThrow();
        assertThat(updated.metadata().homeUri().toString()).isEqualTo("https://client-b.example/home-new");
        assertThat(updated.metadata().callbackUri().toString()).isEqualTo("https://client-b.example/callback-new");
        assertThat(updated.metadata().backchannelLogoutUri().toString())
                .isEqualTo("https://client-b.example/backchannel-logout-new");
        assertThat(updated.metadata().frontchannelLogoutUri().toString())
                .isEqualTo("https://client-b.example/frontchannel-logout-new");
    }

    @Test
    void updateProfile_absentFieldsLeftUnchanged() {
        repo.insert("oidc-app-c", "client-c", "secret-c",
                new byte[]{1}, new byte[]{2},
                "https://client-c.example", "https://client-c.example/callback",
                "desc-c",
                "https://client-c.example/backchannel-logout",
                "https://client-c.example/frontchannel-logout");

        repo.updateProfile("oidc-app-c", "oidc-app-c",
                NullableUpdate.of("https://client-c.example/home-new"),
                NullableUpdate.of("https://client-c.example/callback-new"),
                NullableUpdate.of("desc-c-new"),
                NullableUpdate.absent(),
                NullableUpdate.absent());

        OidcApplication updated = repo.findByName("oidc-app-c").orElseThrow();
        assertThat(updated.metadata().homeUri().toString()).isEqualTo("https://client-c.example/home-new");
        assertThat(updated.metadata().backchannelLogoutUri().toString())
                .isEqualTo("https://client-c.example/backchannel-logout");
        assertThat(updated.metadata().frontchannelLogoutUri().toString())
                .isEqualTo("https://client-c.example/frontchannel-logout");
    }

    @Test
    void updateProfile_clearsNullableFieldsWhenSetToNull() {
        repo.insert("oidc-app-e", "client-e", "secret-e",
                new byte[]{1}, new byte[]{2},
                "https://client-e.example", "https://client-e.example/callback",
                "desc-e",
                "https://client-e.example/backchannel-logout",
                "https://client-e.example/frontchannel-logout");

        repo.updateProfile("oidc-app-e", "oidc-app-e",
                NullableUpdate.of(null),
                NullableUpdate.of(null),
                NullableUpdate.of(null),
                NullableUpdate.of(null),
                NullableUpdate.of(null));

        OidcApplication updated = repo.findByName("oidc-app-e").orElseThrow();
        assertThat(updated.metadata().homeUri()).isNull();
        assertThat(updated.metadata().callbackUri()).isNull();
        assertThat(updated.description()).isNull();
        assertThat(updated.metadata().backchannelLogoutUri()).isNull();
        assertThat(updated.metadata().frontchannelLogoutUri()).isNull();
    }

    @Test
    void updateClientSecret_onlyChangesSecret() {
        repo.insert("oidc-app-f", "client-f", "old-secret",
                new byte[]{1}, new byte[]{2},
                "https://client-f.example", "https://client-f.example/callback",
                "desc-f", null, null);

        repo.updateClientSecret("oidc-app-f", "new-secret");

        OidcApplication updated = repo.findByName("oidc-app-f").orElseThrow();
        assertThat(updated.credentials().clientSecret()).isEqualTo("new-secret");
        assertThat(updated.metadata().homeUri().toString()).isEqualTo("https://client-f.example");
    }

    @Test
    void findByName_treatsBlankLogoutUrisAsNull() {
        repo.insert("oidc-app-d", "client-d", "secret-d",
                new byte[]{1}, new byte[]{2},
                "https://client-d.example", "https://client-d.example/callback",
                "desc-d", "", "   ");

        OidcApplication found = repo.findByName("oidc-app-d").orElseThrow();
        assertThat(found.metadata().backchannelLogoutUri()).isNull();
        assertThat(found.metadata().frontchannelLogoutUri()).isNull();
    }
}
