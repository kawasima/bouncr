package net.unit8.bouncr.api.service;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import net.unit8.bouncr.component.BouncrConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks authentication failures per IP (Layer 1) and per account+IP pair (Layer 2).
 * Uses in-memory sliding window counters with size-capped ConcurrentHashMap.
 */
public class AuthFailureTracker extends SystemComponent<AuthFailureTracker> {
    private static final Logger LOG = LoggerFactory.getLogger(AuthFailureTracker.class);
    private static final int MAX_ENTRIES = 10_000;

    private BouncrConfiguration config;
    private Map<String, SlidingWindowCounter> ipCounters;
    private Map<String, SlidingWindowCounter> accountIpCounters;

    @Override
    protected ComponentLifecycle<AuthFailureTracker> lifecycle() {
        return new ComponentLifecycle<>() {
            @Override
            public void start(AuthFailureTracker component) {
                component.ipCounters = new ConcurrentHashMap<>();
                component.accountIpCounters = new ConcurrentHashMap<>();
            }

            @Override
            public void stop(AuthFailureTracker component) {
                if (component.ipCounters != null) component.ipCounters.clear();
                if (component.accountIpCounters != null) component.accountIpCounters.clear();
            }
        };
    }

    /** Check if the given IP or account+IP pair is currently blocked. */
    public boolean isBlocked(String ip, String account) {
        if (isIpBlocked(ip)) {
            LOG.info("Rate limit: rejected request from blocked IP {}", ip);
            return true;
        }
        if (account != null && isAccountIpBlocked(ip, account)) {
            LOG.info("Rate limit: rejected request from blocked account={} IP={}", account, ip);
            return true;
        }
        return false;
    }

    /** Record a failed authentication attempt. */
    public void recordFailure(String ip, String account) {
        SlidingWindowCounter ipCounter = getOrCreate(ipCounters, ip,
                config.getFailureIpMax(),
                config.getFailureIpWindowSeconds(),
                config.getFailureIpBlockSeconds());
        if (ipCounter.recordFailure()) {
            LOG.warn("Rate limit: IP {} blocked for {} seconds (threshold: {} failures)",
                    ip, config.getFailureIpBlockSeconds(), config.getFailureIpMax());
        }

        if (account != null) {
            String key = account + "|" + ip;
            SlidingWindowCounter accountIpCounter = getOrCreate(accountIpCounters, key,
                    config.getFailureAccountIpMax(),
                    config.getFailureAccountIpWindowSeconds(),
                    config.getFailureAccountIpBlockSeconds());
            if (accountIpCounter.recordFailure()) {
                LOG.warn("Rate limit: account={} IP={} blocked for {} seconds (threshold: {} failures)",
                        account, ip, config.getFailureAccountIpBlockSeconds(),
                        config.getFailureAccountIpMax());
            }
        }
    }

    private SlidingWindowCounter getOrCreate(Map<String, SlidingWindowCounter> map, String key,
                                             int max, int windowSeconds, int blockSeconds) {
        if (map.size() >= MAX_ENTRIES && !map.containsKey(key)) {
            // Map is full and this is a new key: return a throwaway counter to avoid unbounded growth.
            return new SlidingWindowCounter(max, windowSeconds, blockSeconds);
        }
        return map.computeIfAbsent(key, k -> new SlidingWindowCounter(max, windowSeconds, blockSeconds));
    }

    private boolean isIpBlocked(String ip) {
        SlidingWindowCounter counter = ipCounters.get(ip);
        return counter != null && counter.isBlocked();
    }

    private boolean isAccountIpBlocked(String ip, String account) {
        SlidingWindowCounter counter = accountIpCounters.get(account + "|" + ip);
        return counter != null && counter.isBlocked();
    }
}
