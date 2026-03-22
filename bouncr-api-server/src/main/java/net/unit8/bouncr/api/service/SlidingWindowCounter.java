package net.unit8.bouncr.api.service;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Thread-safe sliding window counter that tracks event timestamps
 * and supports blocking when the threshold is exceeded.
 */
public class SlidingWindowCounter {
    private final Deque<Long> timestamps = new ArrayDeque<>();
    private final int maxFailures;
    private final long windowSeconds;
    private final long blockSeconds;
    private long blockedUntil;

    public SlidingWindowCounter(int maxFailures, long windowSeconds, long blockSeconds) {
        this.maxFailures = maxFailures;
        this.windowSeconds = windowSeconds;
        this.blockSeconds = blockSeconds;
    }

    /**
     * Records a failure at the current time.
     * @return true if this failure caused the counter to exceed the threshold (newly blocked)
     */
    public synchronized boolean recordFailure() {
        long now = System.currentTimeMillis() / 1000;
        evict(now);
        timestamps.add(now);
        if (timestamps.size() >= maxFailures && blockedUntil <= now) {
            blockedUntil = now + blockSeconds;
            return true;
        }
        return false;
    }

    public synchronized boolean isBlocked() {
        long now = System.currentTimeMillis() / 1000;
        return blockedUntil > now;
    }

    /** Returns seconds remaining until the block expires, or 0 if not blocked. */
    public synchronized long remainingBlockSeconds() {
        long now = System.currentTimeMillis() / 1000;
        return Math.max(0, blockedUntil - now);
    }

    private void evict(long now) {
        long cutoff = now - windowSeconds;
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }
    }
}
