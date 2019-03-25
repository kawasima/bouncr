package net.unit8.bouncr.api.hook;

import enkan.data.jpa.EntityManageable;
import enkan.exception.MisconfigurationException;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.hook.Hook;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;

public class GrantBouncrUserRole implements Hook<RestContext> {
    @Override
    public void run(RestContext context) {
        EntityManager em = ((EntityManageable) context.getRequest()).getEntityManager();
        if (em == null) {
            throw new MisconfigurationException("bouncr.GRANT_BOUNCR_USER_ROLE_REQUIRES_EM");
        }
        context.getValue(User.class).ifPresent(user -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Group> query = cb.createQuery(Group.class);
            Root<Group> groupRoot = query.from(Group.class);
            query.where(cb.equal(groupRoot.get("name"), "BOUNCR_USER"));
            em.createQuery(query)
                    .getResultStream()
                    .findAny()
                    .ifPresent(bouncrUserGroup -> {
                        if (user.getGroups() == null) {
                            user.setGroups(new ArrayList<>());
                        }
                        user.getGroups().add(bouncrUserGroup);
                    });
        });
    }
}
