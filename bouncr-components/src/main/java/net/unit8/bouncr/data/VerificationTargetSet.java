package net.unit8.bouncr.data;

import java.util.Collection;
import java.util.HashSet;

/**
 * Set wrapper for profile verifications targeted in a verification workflow.
 */
public class VerificationTargetSet extends HashSet<UserProfileVerification> {
    /**
     * Creates a set initialized with verification targets.
     *
     * @param set initial targets
     */
    public VerificationTargetSet(Collection<UserProfileVerification> set) {
        super(set);
    }
}
