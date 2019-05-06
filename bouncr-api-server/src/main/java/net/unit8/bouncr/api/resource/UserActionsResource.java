package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.exception.UnreachableException;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.UserActionSearchParams;
import net.unit8.bouncr.entity.UserAction;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.*;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods("GET")
public class UserActionsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(AUTHORIZED)
    public boolean authorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(ALLOWED)
    public boolean isGetAllowed(UserPermissionPrincipal principal, Parameters params) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("user:read")
                        || p.hasPermission("any_user:read")
                        || (p.hasPermission("my:read") && Objects.equals(p.getName(), params.get("account"))))
                .isPresent();
    }

    @Decision(MALFORMED)
    public Problem validateUserActionSearch(Parameters params, RestContext context) {
        UserActionSearchParams userActionSearchParams = converter.createFrom(params, UserActionSearchParams.class);
        Set<ConstraintViolation<UserActionSearchParams>> violations = validator.validate(userActionSearchParams);
        if (violations.isEmpty()) {
            context.putValue(userActionSearchParams);
        }
        return violations.isEmpty() ? null : builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
    }

    @Decision(HANDLE_OK)
    public List<UserAction> handleOk(UserActionSearchParams params,
                                     UserPermissionPrincipal principal,
                                     EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserAction> query = cb.createQuery(UserAction.class);
        Root<UserAction> userActionRoot = query.from(UserAction.class);
        List<Predicate> predicates = new ArrayList<>();
        Optional<String> maybeActor = Optional.ofNullable(params.getActor());
        if (principal.hasPermission("any_user:read")
                && maybeActor.isPresent()) {
            predicates.add(cb.equal(userActionRoot.get("actor"), maybeActor.orElseThrow(UnreachableException::new)));
        } else {
            predicates.add(cb.equal(userActionRoot.get("actor"), principal.getName()));
        }

        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }
        query.orderBy(cb.desc(userActionRoot.get("createdAt")));

        return em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }
}
