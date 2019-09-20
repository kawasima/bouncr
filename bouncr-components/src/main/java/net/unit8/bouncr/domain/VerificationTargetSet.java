package net.unit8.bouncr.domain;

import net.unit8.bouncr.entity.UserProfileVerification;

import java.util.Collection;
import java.util.HashSet;

public class VerificationTargetSet extends HashSet<UserProfileVerification> {
    public VerificationTargetSet(Collection<UserProfileVerification> set) {
        super(set);
    }
}
