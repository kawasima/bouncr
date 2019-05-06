package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.data.HttpRequest;
import enkan.exception.UnreachableException;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.PermissionUpdateRequest;
import net.unit8.bouncr.api.service.UniquenessCheckService;
import net.unit8.bouncr.entity.Permission;
import net.unit8.bouncr.entity.User;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import javax.validation.ConstraintViolation;
import java.util.*;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class PermissionResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdateRequest(PermissionUpdateRequest updateRequest, RestContext context) {
        if (updateRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        Set<ConstraintViolation<PermissionUpdateRequest>> violations = validator.validate(updateRequest);
        return violations.isEmpty() ? null : builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("permission:read") || p.hasPermission("any_permission:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("permission:update") || p.hasPermission("any_permission:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("permission:delete") || p.hasPermission("any_permission:delete"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(PermissionUpdateRequest updateRequest, Parameters params, EntityManager em) {
        if (Objects.equals(updateRequest.getName(), params.get("name"))) {
            return false;
        }
        UniquenessCheckService<Permission> uniquenessCheckService = new UniquenessCheckService<>(em);
        return !uniquenessCheckService.isUnique(Permission.class, "nameLower",
                Optional.ofNullable(updateRequest.getName())
                        .map(n -> n.toLowerCase(Locale.US))
                        .orElseThrow(UnreachableException::new));
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params,
                          UserPermissionPrincipal principal,
                          HttpRequest request,
                          RestContext context,
                          EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Permission> query = cb.createQuery(Permission.class);
        Root<Permission> permissionRoot = query.from(Permission.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(permissionRoot.get("name"), params.get("name")));
        if ((request.getRequestMethod().equalsIgnoreCase("GET") && !principal.hasPermission("any_permission:read"))
                || (request.getRequestMethod().equalsIgnoreCase("PUT") && !principal.hasPermission("any_permission:update"))
                || (request.getRequestMethod().equalsIgnoreCase("DELETE") && !principal.hasPermission("any_permission:delete"))) {
            Join<Permission, User> userRoot = permissionRoot.join("roles")
                    .join("assignments")
                    .join("groups")
                    .join("users");
            predicates.add(cb.equal(userRoot.get("id"), principal.getId()));
        }

        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }

        query.where(cb.equal(permissionRoot.get("name"), params.get("name")));

        Permission permission = em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);
        if (permission != null) {
            context.putValue(permission);
        }
        return permission != null;
    }

    @Decision(HANDLE_OK)
    public Permission find(Permission permission, EntityManager em) {
        em.detach(permission);
        return permission;
    }

    @Decision(PUT)
    public Permission update(PermissionUpdateRequest updateRequest, Permission permission, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> converter.copy(updateRequest, permission));
        em.detach(permission);
        return permission;
    }

    @Decision(DELETE)
    public Void delete(Permission permission, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(permission));
        return null;
    }

}
