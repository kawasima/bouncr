package net.unit8.bouncr.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainPrimitiveTest {

    @Test
    void wordName_holdsValue() {
        var name = new WordName("admin");
        assertThat(name.value()).isEqualTo("admin");
    }

    @Test
    void wordName_equality() {
        assertThat(new WordName("admin")).isEqualTo(new WordName("admin"));
        assertThat(new WordName("admin")).isNotEqualTo(new WordName("guest"));
    }

    @Test
    void permissionName_holdsValue() {
        var name = new PermissionName("oidc_application:read");
        assertThat(name.value()).isEqualTo("oidc_application:read");
    }

    @Test
    void permissionName_equality() {
        assertThat(new PermissionName("user:read")).isEqualTo(new PermissionName("user:read"));
        assertThat(new PermissionName("user:read")).isNotEqualTo(new PermissionName("user:write"));
    }

    @Test
    void email_holdsValue() {
        var email = new Email("alice@example.com");
        assertThat(email.value()).isEqualTo("alice@example.com");
    }

    @Test
    void email_equality() {
        assertThat(new Email("a@b.com")).isEqualTo(new Email("a@b.com"));
        assertThat(new Email("a@b.com")).isNotEqualTo(new Email("x@y.com"));
    }
}
