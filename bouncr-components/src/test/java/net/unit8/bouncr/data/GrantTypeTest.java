package net.unit8.bouncr.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrantTypeTest {

    @Test
    void getValue_returnsWireFormatString() {
        assertThat(GrantType.AUTHORIZATION_CODE.getValue()).isEqualTo("authorization_code");
        assertThat(GrantType.REFRESH_TOKEN.getValue()).isEqualTo("refresh_token");
        assertThat(GrantType.CLIENT_CREDENTIALS.getValue()).isEqualTo("client_credentials");
    }

    @ParameterizedTest
    @CsvSource({
            "authorization_code, AUTHORIZATION_CODE",
            "refresh_token,      REFRESH_TOKEN",
            "client_credentials, CLIENT_CREDENTIALS"
    })
    void fromString_validValues(String wire, GrantType expected) {
        assertThat(GrantType.fromString(wire)).hasValue(expected);
    }

    @Test
    void fromString_unknownValue_returnsEmpty() {
        assertThat(GrantType.fromString("implicit")).isEmpty();
        assertThat(GrantType.fromString("")).isEmpty();
        assertThat(GrantType.fromString("AUTHORIZATION_CODE")).isEmpty();
    }

    @Test
    void parseAll_validGrantTypes() {
        Set<GrantType> result = GrantType.parseAll(
                List.of("authorization_code", "refresh_token"));

        assertThat(result).containsExactlyInAnyOrder(
                GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN);
    }

    @Test
    void parseAll_allGrantTypes() {
        Set<GrantType> result = GrantType.parseAll(
                List.of("authorization_code", "refresh_token", "client_credentials"));

        assertThat(result).containsExactlyInAnyOrder(GrantType.values());
    }

    @Test
    void parseAll_unknownGrantType_throws() {
        assertThatThrownBy(() -> GrantType.parseAll(List.of("implicit")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown grant_type: implicit");
    }

    @Test
    void parseAll_mixedValidAndInvalid_throws() {
        assertThatThrownBy(() ->
                GrantType.parseAll(List.of("authorization_code", "device_code")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("device_code");
    }

    @Test
    void parseAll_emptyList_returnsEmptySet() {
        Set<GrantType> result = GrantType.parseAll(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void parseAll_duplicates_deduplicated() {
        Set<GrantType> result = GrantType.parseAll(
                List.of("authorization_code", "authorization_code"));
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(GrantType.AUTHORIZATION_CODE);
    }

    @Test
    void defaultGrantTypes_containsAllValues() {
        assertThat(GrantType.DEFAULT_GRANT_TYPES).containsExactly(
                "authorization_code", "refresh_token", "client_credentials");
    }

    @Test
    void allEnumValues_coveredByDefaultGrantTypes() {
        for (GrantType gt : GrantType.values()) {
            assertThat(GrantType.DEFAULT_GRANT_TYPES).contains(gt.getValue());
        }
    }
}
