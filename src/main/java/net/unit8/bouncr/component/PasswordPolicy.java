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
}
