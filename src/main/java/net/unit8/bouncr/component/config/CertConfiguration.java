package net.unit8.bouncr.component.config;

import java.io.Serializable;
import java.time.Duration;

public class CertConfiguration implements Serializable {
    private Duration defaultExpiry = Duration.ofDays(180L);
    private int keyLength = 2048;
    private String signAlgorithm = "SHA256WithRSAEncryption";

    public Duration getDefaultExpiry() {
        return defaultExpiry;
    }

    public void setDefaultExpiry(Duration defaultExpiry) {
        this.defaultExpiry = defaultExpiry;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(int keyLength) {
        this.keyLength = keyLength;
    }

    public String getSignAlgorithm() {
        return signAlgorithm;
    }

    public void setSignAlgorithm(String signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }
}
