package net.unit8.bouncr.json;

import org.eclipse.persistence.indirection.IndirectList;

public class IndirectListFilter {
    @Override
    public boolean equals(Object obj) {
        return obj instanceof IndirectList && !((IndirectList<?>) obj).isInstantiated();
    }
}
