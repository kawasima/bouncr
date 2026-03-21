package net.unit8.bouncr.api.authn;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientAuthenticateMiddlewareTest {
    @Test
    void extractCommonName_readsCnFromDn() {
        assertThat(ClientAuthenticateMiddleware.extractCommonName("CN=alice,OU=Dev,O=Unit8,C=JP"))
                .isEqualTo("alice");
    }

    @Test
    void extractCommonName_returnsNullWhenDnInvalid() {
        assertThat(ClientAuthenticateMiddleware.extractCommonName("not-a-dn"))
                .isNull();
    }
}
