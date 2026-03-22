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
    void remainingBlockSecondsPositiveWhenBlocked() {
        SlidingWindowCounter counter = new SlidingWindowCounter(1, 60, 300);
        counter.recordFailure();
        assertThat(counter.remainingBlockSeconds()).isGreaterThan(0);
        assertThat(counter.remainingBlockSeconds()).isLessThanOrEqualTo(300);
    }

    @Test
    void remainingBlockSecondsZeroWhenNotBlocked() {
        SlidingWindowCounter counter = new SlidingWindowCounter(3, 60, 300);
        assertThat(counter.remainingBlockSeconds()).isEqualTo(0);
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
