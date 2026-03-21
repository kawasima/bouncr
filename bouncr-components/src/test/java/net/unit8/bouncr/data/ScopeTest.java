package net.unit8.bouncr.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeTest {

    @Test
    void parse_spaceSeparatedString() {
        Scope scope = Scope.parse("openid profile email");
        assertThat(scope.contains("openid")).isTrue();
        assertThat(scope.contains("profile")).isTrue();
        assertThat(scope.contains("email")).isTrue();
        assertThat(scope.contains("admin")).isFalse();
    }

    @Test
    void parse_null_returnsEmptyScope() {
        Scope scope = Scope.parse(null);
        assertThat(scope.values()).isEmpty();
    }

    @Test
    void parse_blank_returnsEmptyScope() {
        Scope scope = Scope.parse("   ");
        assertThat(scope.values()).isEmpty();
    }

    @Test
    void parse_duplicates_deduplicated() {
        Scope scope = Scope.parse("openid openid profile");
        assertThat(scope.values()).hasSize(2);
    }

    @Test
    void isSubsetOf_subsetIsTrue() {
        Scope small = Scope.parse("openid profile");
        Scope large = Scope.parse("openid profile email");
        assertThat(small.isSubsetOf(large)).isTrue();
    }

    @Test
    void isSubsetOf_supersetIsFalse() {
        Scope large = Scope.parse("openid profile email");
        Scope small = Scope.parse("openid profile");
        assertThat(large.isSubsetOf(small)).isFalse();
    }

    @Test
    void isSubsetOf_equalIsTrue() {
        Scope a = Scope.parse("openid profile");
        Scope b = Scope.parse("openid profile");
        assertThat(a.isSubsetOf(b)).isTrue();
    }

    @Test
    void toString_spaceSeparated() {
        Scope scope = Scope.parse("openid profile");
        String str = scope.toString();
        assertThat(str).contains("openid");
        assertThat(str).contains("profile");
    }
}
