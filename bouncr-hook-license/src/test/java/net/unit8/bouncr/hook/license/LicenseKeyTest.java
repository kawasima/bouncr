package net.unit8.bouncr.hook.license;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LicenseKeyTest {
    @Test
    void aLicenseEqualsTheLicenseFromBytes() {
        final LicenseKey aNew = LicenseKey.createNew();
        final LicenseKey fromBytes = new LicenseKey(aNew.asBytes());
        assertThat(fromBytes).isEqualTo(aNew);
    }

    @Test
    void aLicenseEqualsTheLicenseFromString() {
        final LicenseKey aNew = LicenseKey.createNew();
        final LicenseKey fromString = new LicenseKey(aNew.asString());
        assertThat(fromString).isEqualTo(aNew);
    }

}