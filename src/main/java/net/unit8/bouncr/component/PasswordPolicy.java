package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;

import java.time.Duration;

public class PasswordPolicy extends SystemComponent {
    private Duration expires;
    private int minLength = 8;
    private int maxLength = 128;
    private int numOfTrialsUntilLock = 10;

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<PasswordPolicy>() {
            @Override
            public void start(PasswordPolicy policy) {

            }

            @Override
            public void stop(PasswordPolicy policy) {

            }
        };
    }

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

    public int getNumOfTrialsUntilLock() {
        return numOfTrialsUntilLock;
    }

    public void setNumOfTrialsUntilLock(int numOfTrialsUntilLock) {
        this.numOfTrialsUntilLock = numOfTrialsUntilLock;
    }
}
