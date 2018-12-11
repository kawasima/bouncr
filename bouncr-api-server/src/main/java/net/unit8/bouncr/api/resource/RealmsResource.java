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
import net.unit8.bouncr.api.boundary.RealmCreateRequest;
import net.unit8.bouncr.api.boundary.RealmSearchParams;
import net.unit8.bouncr.entity.Application;
import net.unit8.bouncr.entity.Realm;

import javax.inject.Inject;
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
public class RealmsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(IS_AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = IS_ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal, HttpRequest request) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("LIST_REALMS") || p.hasPermission("LIST_ANY_REALMS"))
                .isPresent();
    }

    @Decision(value = IS_ALLOWED, method= "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal, HttpRequest request) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("CREATE_REALM") || p.hasPermission("CREATE_ANY_REALM"))
                .isPresent();
    }
    @Decision(value = MALFORMED, method = "POST")
    public Problem validateApplicationCreateRequest(RealmCreateRequest createRequest, RestContext context) {
        Set<ConstraintViolation<RealmCreateRequest>> violations = validator.validate(createRequest);
        if (violations.isEmpty()) {
            context.putValue(converter.createFrom(createRequest, Realm.class));
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(value = MALFORMED, method = "GET")
    public Problem validateApplicationSearchParams(Parameters params, RestContext context) {
        RealmSearchParams applicationSearchParams = converter.createFrom(params, RealmSearchParams.class);
        Set<ConstraintViolation<RealmSearchParams>> violations = validator.validate(applicationSearchParams);
        if (violations.isEmpty()) {
            context.putValue(converter.createFrom(applicationSearchParams, RealmSearchParams.class));
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(PROCESSABLE)
    public boolean isProcessable(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Application> query = cb.createQuery(Application.class);
        Root<Application> applicationRoot = query.from(Application.class);
        query.where(cb.equal(applicationRoot.get("name"), params.get("name")));

        Application application = em.createQuery(query).getResultStream().findAny().orElse(null);
        if (application != null) {
            context.putValue(application);
        }
        return application != null;
    }

    @Decision(POST)
    public Realm create(Realm realm, Application application, EntityManager em) {
        realm.setApplication(application);
        realm.setWriteProtected(false);

        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.persist(realm));
        em.detach(realm);
        return realm;
    }

    @Decision(HANDLE_OK)
    public List<Realm> list(RealmSearchParams params, Application application, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Realm> query = cb.createQuery(Realm.class);
        Root<Realm> realmRoot = query.from(Realm.class);
        Join<Application, Realm> applicationJoin = realmRoot.join("application");
        query.where(cb.equal(applicationJoin.get("id"), application.getId()));

        return em.createQuery(query)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }
}
