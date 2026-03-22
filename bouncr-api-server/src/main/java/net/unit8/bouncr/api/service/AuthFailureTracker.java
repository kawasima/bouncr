package net.unit8.bouncr.api.service;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import net.unit8.bouncr.component.BouncrConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks authentication failures per IP (Layer 1) and per account+IP pair (Layer 2).
 * Uses in-memory sliding window counters with LRU eviction.
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
                component.ipCounters = Collections.synchronizedMap(
                        new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
                            @Override
                            protected boolean removeEldestEntry(Map.Entry<String, SlidingWindowCounter> eldest) {
                                return size() > MAX_ENTRIES;
                            }
                        });
                component.accountIpCounters = Collections.synchronizedMap(
                        new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
                            @Override
                            protected boolean removeEldestEntry(Map.Entry<String, SlidingWindowCounter> eldest) {
                                return size() > MAX_ENTRIES;
                            }
                        });
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
        SlidingWindowCounter ipCounter = ipCounters.computeIfAbsent(ip,
                k -> new SlidingWindowCounter(
                        config.getFailureIpMax(),
                        config.getFailureIpWindowSeconds(),
                        config.getFailureIpBlockSeconds()));
        if (ipCounter.recordFailure()) {
            LOG.warn("Rate limit: IP {} blocked for {} seconds (failures: {})",
                    ip, config.getFailureIpBlockSeconds(), config.getFailureIpMax());
        }

        if (account != null) {
            String key = account + "|" + ip;
            SlidingWindowCounter accountIpCounter = accountIpCounters.computeIfAbsent(key,
                    k -> new SlidingWindowCounter(
                            config.getFailureAccountIpMax(),
                            config.getFailureAccountIpWindowSeconds(),
                            config.getFailureAccountIpBlockSeconds()));
            if (accountIpCounter.recordFailure()) {
                LOG.warn("Rate limit: account={} IP={} blocked for {} seconds (failures: {})",
                        account, ip, config.getFailureAccountIpBlockSeconds(),
                        config.getFailureAccountIpMax());
            }
        }
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
