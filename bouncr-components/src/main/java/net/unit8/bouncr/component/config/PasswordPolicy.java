package net.unit8.bouncr.component.config;

import java.io.Serializable;
import java.time.Duration;
import java.util.regex.Pattern;

public class PasswordPolicy implements Serializable {
    private Duration expires;
    private int minLength = 8;
    private int maxLength = 128;
    private Pattern pattern = Pattern.compile("[\\p{Punct}\\p{Alnum}]+");
    private int numOfTrialsUntilLock = 10;

    public Duration getExpires() {
        return expires;
    }

    public void setExpires(Duration expires) {
        this.expires = expires;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public int getNumOfTrialsUntilLock() {
        return numOfTrialsUntilLock;
    }

    public void setNumOfTrialsUntilLock(int numOfTrialsUntilLock) {
        this.numOfTrialsUntilLock = numOfTrialsUntilLock;
    }
}
