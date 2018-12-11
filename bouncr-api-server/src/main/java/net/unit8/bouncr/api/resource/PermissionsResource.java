package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.PermissionCreateRequest;
import net.unit8.bouncr.api.boundary.PermissionSearchParams;
import net.unit8.bouncr.entity.Permission;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class PermissionsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(IS_AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = IS_ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("LIST_PERMISSIONS") || p.hasPermission("LIST_ANY_PERMISSIONS"))
                .isPresent();
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem validatePermissionCreateRequest(PermissionCreateRequest createRequest, RestContext context) {
        Set<ConstraintViolation<PermissionCreateRequest>> violations = validator.validate(createRequest);
        if (violations.isEmpty()) {
            context.putValue(converter.createFrom(createRequest, Permission.class));
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(value = MALFORMED, method = "GET")
    public Problem validatePermissionSearchParams(Parameters params, RestContext context) {
        PermissionSearchParams permissionSearchParams = converter.createFrom(params, PermissionSearchParams.class);
        Set<ConstraintViolation<PermissionSearchParams>> violations = validator.validate(permissionSearchParams);
        if (violations.isEmpty()) {
            context.putValue(converter.createFrom(permissionSearchParams, PermissionSearchParams.class));
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(HANDLE_OK)
    public List<Permission> list(PermissionSearchParams params, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Permission> query = cb.createQuery(Permission.class);
        query.from(Permission.class);

        return em.createQuery(query)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }
}
