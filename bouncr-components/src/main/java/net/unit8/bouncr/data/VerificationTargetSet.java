package net.unit8.bouncr.data;

import java.util.Collection;
import java.util.HashSet;

public class VerificationTargetSet extends HashSet<UserProfileVerification> {
    public VerificationTargetSet(Collection<UserProfileVerification> set) {
        super(set);
    }
}
