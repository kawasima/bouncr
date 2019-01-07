package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.data.HttpRequest;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.RoleCreateRequest;
import net.unit8.bouncr.api.boundary.RoleSearchParams;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.Role;
import net.unit8.bouncr.entity.User;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class RolesResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal, HttpRequest request) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("LIST_ROLES") || p.hasPermission("LIST_ANY_ROLES"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal, HttpRequest request) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("CREATE_ROLE"))
                .isPresent();
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateRoleCreateRequest(RoleCreateRequest createRequest, RestContext context) {
        Set<ConstraintViolation<RoleCreateRequest>> violations = validator.validate(createRequest);
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(value = MALFORMED, method = "GET")
    public Problem validateRoleSearchParams(Parameters params, RestContext context) {
        RoleSearchParams applicationSearchParams = converter.createFrom(params, RoleSearchParams.class);
        Set<ConstraintViolation<RoleSearchParams>> violations = validator.validate(applicationSearchParams);
        if (violations.isEmpty()) {
            context.putValue(converter.createFrom(applicationSearchParams, RoleSearchParams.class));
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(HANDLE_OK)
    public List<Role> handleOk(RoleSearchParams params, UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> query = cb.createQuery(Role.class);
        Root<Role> roleRoot = query.from(Role.class);

        if (!principal.hasPermission("LIST_ANY_ROLES")) {
            Join<User, Group> joinUsers = roleRoot.join("assignments")
                    .join("group")
                    .join("users");
            query.where(cb.equal(joinUsers.get("id"), principal.getId()));
        }
        query.orderBy(cb.asc(roleRoot.get("id")));

        return em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }

    @Decision(POST)
    public Role craete(RoleCreateRequest createRequest, EntityManager em) {
        Role role = converter.createFrom(createRequest, Role.class);
        role.setWriteProtected(false);
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.persist(role));
        em.detach(role);
        return role;
    }
}
