package net.unit8.bouncr.api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingWindowCounterTest {
    @Test
    void notBlockedInitially() {
        SlidingWindowCounter counter = new SlidingWindowCounter(3, 60, 300);
        assertThat(counter.isBlocked()).isFalse();
    }

    @Test
    void blocksAfterThresholdReached() {
        SlidingWindowCounter counter = new SlidingWindowCounter(3, 60, 300);
        counter.recordFailure();
        counter.recordFailure();
        assertThat(counter.isBlocked()).isFalse();
        boolean newlyBlocked = counter.recordFailure();
        assertThat(newlyBlocked).isTrue();
        assertThat(counter.isBlocked()).isTrue();
    }

    @Test
    void recordFailureReturnsFalseUntilThreshold() {
        SlidingWindowCounter counter = new SlidingWindowCounter(3, 60, 300);
        assertThat(counter.recordFailure()).isFalse();
        assertThat(counter.recordFailure()).isFalse();
        assertThat(counter.recordFailure()).isTrue();
    }

    @Test
    void recordFailureReturnsFalseOnceAlreadyBlocked() {
        SlidingWindowCounter counter = new SlidingWindowCounter(2, 60, 300);
        counter.recordFailure();
        counter.recordFailure(); // now blocked
        // additional failures while already blocked should not re-trigger
        assertThat(counter.recordFailure()).isFalse();
    }
}
