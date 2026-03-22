package net.unit8.bouncr.api.service;

import net.unit8.bouncr.component.BouncrConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFailureTrackerTest {
    private AuthFailureTracker tracker;

    @BeforeEach
    void setup() {
        BouncrConfiguration config = new BouncrConfiguration();
        // low thresholds so tests trigger quickly
        config.setFailureIpMax(3);
        config.setFailureIpWindowSeconds(600);
        config.setFailureIpBlockSeconds(900);
        config.setFailureAccountIpMax(2);
        config.setFailureAccountIpWindowSeconds(300);
        config.setFailureAccountIpBlockSeconds(600);

        tracker = new AuthFailureTracker() {{
            lifecycle().start(this);
        }};
        setField(tracker, "config", config);
    }

    @AfterEach
    void teardown() {
        if (tracker != null) tracker.lifecycle().stop(tracker);
    }

    @Test
    void notBlockedInitially() {
        assertThat(tracker.isBlocked("1.2.3.4", "alice")).isFalse();
    }

    @Test
    void ipBlockedAfterThreshold() {
        tracker.recordFailure("1.2.3.4", null);
        tracker.recordFailure("1.2.3.4", null);
        assertThat(tracker.isBlocked("1.2.3.4", null)).isFalse();
        tracker.recordFailure("1.2.3.4", null);
        assertThat(tracker.isBlocked("1.2.3.4", null)).isTrue();
    }

    @Test
    void accountIpBlockedAfterThreshold() {
        tracker.recordFailure("1.2.3.4", "alice");
        assertThat(tracker.isBlocked("1.2.3.4", "alice")).isFalse();
        tracker.recordFailure("1.2.3.4", "alice");
        assertThat(tracker.isBlocked("1.2.3.4", "alice")).isTrue();
    }

    @Test
    void differentIpsAreIndependent() {
        tracker.recordFailure("1.2.3.4", null);
        tracker.recordFailure("1.2.3.4", null);
        tracker.recordFailure("1.2.3.4", null); // blocked
        assertThat(tracker.isBlocked("5.6.7.8", null)).isFalse();
    }

    @Test
    void differentAccountsSameIpAreIndependent() {
        tracker.recordFailure("1.2.3.4", "alice");
        tracker.recordFailure("1.2.3.4", "alice"); // alice blocked
        assertThat(tracker.isBlocked("1.2.3.4", "bob")).isFalse();
    }

    @Test
    void nullAccountOnlyChecksIp() {
        // No IP failures — should not be blocked regardless of account
        assertThat(tracker.isBlocked("1.2.3.4", null)).isFalse();
    }

    @Test
    void additionalFailuresAfterBlockDoNotResetBlockedUntil() {
        // Exhaust the account+IP threshold (2 failures)
        tracker.recordFailure("1.2.3.4", "alice");
        tracker.recordFailure("1.2.3.4", "alice");
        assertThat(tracker.isBlocked("1.2.3.4", "alice")).isTrue();
        // Extra failures while already blocked must not shorten the block
        tracker.recordFailure("1.2.3.4", "alice");
        assertThat(tracker.isBlocked("1.2.3.4", "alice")).isTrue();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
