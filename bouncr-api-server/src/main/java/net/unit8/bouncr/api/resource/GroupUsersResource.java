package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.GroupUsersCreateRequest;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.User;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST", "DELETE"})
public class GroupUsersResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("LIST_GROUPS") || p.hasPermission("LIST_ANY_GROUPS"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= { "POST", "DELETE" })
    public boolean isModifyAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("MODIFY_GROUP") || p.hasPermission("MODIFY_ANY_GROUP"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> query = cb.createQuery(Group.class);
        Root<Group> groupRoot = query.from(Group.class);
        query.where(cb.equal(groupRoot.get("name"), params.get("name")));

        Group group = em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);
        if (group != null) {
            context.putValue(group);
        }
        return group != null;
    }

    @Decision(HANDLE_OK)
    public List<User> list(Group group, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> userRoot = query.from(User.class);
        Join<Group, User> groupsJoin = userRoot.join("groups");
        query.where(cb.equal(groupsJoin.get("id"), group));
        query.orderBy(cb.asc(userRoot.get("id")));
        return em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultList();
    }

    @Decision(POST)
    public GroupUsersCreateRequest create(GroupUsersCreateRequest createRequest, Group group, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> userRoot = query.from(User.class);
        query.where(userRoot.get("account").in(createRequest.getUsers()));
        List<User> users = em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultList();

        EntityTransactionManager tx = new EntityTransactionManager(em);

        tx.required(() -> {
            HashSet<User> groupUsers = new HashSet<>(group.getUsers());
            groupUsers.addAll(users);
            group.setUsers(new ArrayList<>(groupUsers));
        });

        return createRequest;
    }

    @Decision(DELETE)
    public GroupUsersCreateRequest delete(GroupUsersCreateRequest createRequest, Group group, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> userRoot = query.from(User.class);
        query.where(userRoot.get("account").in(createRequest.getUsers()));
        List<User> users = em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultList();

        EntityTransactionManager tx = new EntityTransactionManager(em);

        tx.required(() -> {
            HashSet<User> groupUsers = new HashSet<>(group.getUsers());
            groupUsers.removeAll(users);
            group.setUsers(new ArrayList<>(groupUsers));
        });

        return createRequest;
    }
}
