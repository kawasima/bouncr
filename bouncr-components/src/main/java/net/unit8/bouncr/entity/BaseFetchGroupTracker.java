package net.unit8.bouncr.entity;

import org.eclipse.persistence.queries.FetchGroup;
import org.eclipse.persistence.queries.FetchGroupTracker;
import org.eclipse.persistence.sessions.Session;

import javax.persistence.Transient;

public abstract class BaseFetchGroupTracker implements FetchGroupTracker {
    @Transient
    private FetchGroup fetchGroup;
    @Transient
    private Session session;

    @Override
    public FetchGroup _persistence_getFetchGroup() {
        return fetchGroup;
    }

    @Override
    public void _persistence_setFetchGroup(FetchGroup group) {
        this.fetchGroup = group;
    }

    @Override
    public boolean _persistence_isAttributeFetched(String attribute) {
        return false;
    }

    @Override
    public void _persistence_resetFetchGroup() {

    }

    @Override
    public boolean _persistence_shouldRefreshFetchGroup() {
        return false;
    }

    @Override
    public void _persistence_setShouldRefreshFetchGroup(boolean shouldRefreshFetchGroup) {

    }

    @Override
    public Session _persistence_getSession() {
        return session;
    }

    @Override
    public void _persistence_setSession(Session session) {
        this.session = session;
    }
}
