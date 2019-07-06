package net.unit8.bouncr.hook.license;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class LicenseKey implements Serializable {
    private byte[] keyArray;

    public LicenseKey(String licenseKey) {
        assert(licenseKey != null);
        UUID uuid = UUID.fromString(licenseKey);
        keyArray = uuid2bytes(uuid);
    }

    public LicenseKey(byte[] licenseKey) {
        assert(licenseKey != null);
        keyArray = licenseKey;
    }

    public String asString() {
        ByteBuffer bb = ByteBuffer.wrap(keyArray);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong).toString();
    }

    public byte[] asBytes() {
        return keyArray;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        return Optional.of(obj)
                .filter(LicenseKey.class::isInstance)
                .map(LicenseKey.class::cast)
                .map(lic -> Arrays.equals(keyArray, lic.asBytes()))
                .orElse(false);
    }

    public static LicenseKey createNew() {
        UUID uuid = UUID.randomUUID();
        return new LicenseKey(uuid2bytes(uuid));
    }

    private static byte[] uuid2bytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
