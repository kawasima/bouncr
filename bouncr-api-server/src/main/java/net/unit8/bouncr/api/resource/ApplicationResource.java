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
import net.unit8.apistandard.resourcefilter.ResourceField;
import net.unit8.apistandard.resourcefilter.ResourceFilter;
import net.unit8.bouncr.api.boundary.ApplicationUpdateRequest;
import net.unit8.bouncr.entity.Application;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "PUT", "DELETE"})
public class ApplicationResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "PUT")
    public Problem validateUpdateRequest(ApplicationUpdateRequest updateRequest, RestContext context) {
        Set<ConstraintViolation<ApplicationUpdateRequest>> violations = validator.validate(updateRequest);
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal, HttpRequest request) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("LIST_APPLICATIONS") || p.hasPermission("LIST_ANY_APPLICATIONS"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("MODIFY_APPLICATION") || p.hasPermission("MODIFY_ANY_APPLICATION"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("DELETE_APPLICATION") || p.hasPermission("DELETE_ANY_APPLICATION"))
                .isPresent();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Application> query = cb.createQuery(Application.class);
        Root<Application> applicationRoot = query.from(Application.class);
        query.where(cb.equal(applicationRoot.get("name"), params.get("name")));

        List<ResourceField> embedEntities = some(params.get("embed"), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());

        EntityGraph<Application> applicationGraph = em.createEntityGraph(Application.class);
        applicationGraph.addAttributeNodes("name", "description", "virtualPath", "passTo", "topPage");

        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("realms"))) {
            applicationRoot.fetch("realms", JoinType.LEFT);
            query.distinct(true);
            applicationGraph.addSubgraph("realms")
                    .addAttributeNodes("name", "description", "url");

        }

        Application application = em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setHint("javax.persistence.fetchgraph", applicationGraph)
                .getResultStream().findAny().orElse(null);
        if (application != null) {
            context.putValue(application);
        }
        return application != null;
    }

    @Decision(HANDLE_OK)
    public Application find(Application application, EntityManager em) {
        em.detach(application);
        return application;
    }

    @Decision(PUT)
    public Application update(ApplicationUpdateRequest updateRequest, Application application, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> converter.copy(updateRequest, application));
        em.detach(application);
        return application;
    }

    @Decision(DELETE)
    public Void delete(Application application, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(application));
        return null;
    }
}
