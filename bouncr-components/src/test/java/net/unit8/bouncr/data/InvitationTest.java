package net.unit8.bouncr.data;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationTest {

    @Test
    void construction_allFieldsAccessible() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 24, 12, 0, 0);
        Invitation invitation = new Invitation(
                1L, "INV-001", "alice@example.com", now,
                List.of(), List.of());

        assertThat(invitation.id()).isEqualTo(1L);
        assertThat(invitation.code()).isEqualTo("INV-001");
        assertThat(invitation.email()).isEqualTo("alice@example.com");
        assertThat(invitation.invitedAt()).isEqualTo(now);
        assertThat(invitation.groupInvitations()).isEmpty();
        assertThat(invitation.oidcInvitations()).isEmpty();
    }

    @Test
    void withGroupInvitations() {
        Invitation invitation = new Invitation(
                1L, "INV-002", "bob@example.com",
                LocalDateTime.now(), List.of(), List.of());

        Group group = new GroupPure(10L, new GroupSpec(new WordName("developers"), "Dev team"), false);
        GroupInvitation gi = new GroupInvitation(100L, invitation, group);

        Invitation withGroups = new Invitation(
                invitation.id(), invitation.code(), invitation.email(),
                invitation.invitedAt(), List.of(gi), invitation.oidcInvitations());

        assertThat(withGroups.groupInvitations()).hasSize(1);
        assertThat(withGroups.groupInvitations().get(0).group().name()).isEqualTo(new WordName("developers"));
        assertThat(withGroups.groupInvitations().get(0).invitation()).isSameAs(invitation);
    }

    @Test
    void withOidcInvitations() {
        Invitation invitation = new Invitation(
                2L, "INV-003", "carol@example.com",
                LocalDateTime.now(), List.of(), List.of());

        OidcProvider provider = new OidcProvider(
                20L, "Google", "google",
                new OidcProviderMetadata(
                        "https://accounts.google.com/o/oauth2/v2/auth",
                        "https://oauth2.googleapis.com/token",
                        null, "https://accounts.google.com"),
                new OidcProviderClientConfig(
                        new ClientCredentials("client-id", "client-secret"),
                        "openid profile", ResponseType.CODE,
                        TokenEndpointAuthMethod.CLIENT_SECRET_BASIC,
                        java.net.URI.create("https://example.com/callback"), true));

        OidcInvitation oi = new OidcInvitation(200L, invitation, provider, "{\"sub\":\"12345\"}");

        Invitation withOidc = new Invitation(
                invitation.id(), invitation.code(), invitation.email(),
                invitation.invitedAt(), invitation.groupInvitations(), List.of(oi));

        assertThat(withOidc.oidcInvitations()).hasSize(1);
        assertThat(withOidc.oidcInvitations().get(0).oidcProvider().name()).isEqualTo("Google");
        assertThat(withOidc.oidcInvitations().get(0).oidcPayload()).isEqualTo("{\"sub\":\"12345\"}");
    }

    @Test
    void groupInvitation_backReference() {
        Invitation invitation = new Invitation(
                3L, "INV-004", "dave@example.com",
                LocalDateTime.now(), List.of(), List.of());

        Group group = new GroupPure(30L, new GroupSpec(new WordName("admins"), "Administrators"), true);
        GroupInvitation gi = new GroupInvitation(300L, invitation, group);

        assertThat(gi.id()).isEqualTo(300L);
        assertThat(gi.invitation().code()).isEqualTo("INV-004");
        assertThat(gi.group().writeProtected()).isTrue();
    }

    @Test
    void nullLists_allowed() {
        Invitation invitation = new Invitation(
                4L, "INV-005", "eve@example.com",
                LocalDateTime.now(), null, null);

        assertThat(invitation.groupInvitations()).isNull();
        assertThat(invitation.oidcInvitations()).isNull();
    }

    @Test
    void equality_sameValuesMeansEqual() {
        LocalDateTime ts = LocalDateTime.of(2026, 1, 1, 0, 0);
        Invitation a = new Invitation(1L, "CODE", "a@b.com", ts, List.of(), List.of());
        Invitation b = new Invitation(1L, "CODE", "a@b.com", ts, List.of(), List.of());

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
