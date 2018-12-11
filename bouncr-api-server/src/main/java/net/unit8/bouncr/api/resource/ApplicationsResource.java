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
import net.unit8.bouncr.api.boundary.ApplicationCreateRequest;
import net.unit8.bouncr.api.boundary.ApplicationSearchParams;
import net.unit8.bouncr.entity.Application;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.Realm;
import net.unit8.bouncr.entity.User;

import javax.inject.Inject;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class ApplicationsResource {
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
                .filter(p -> p.hasPermission("LIST_APPLICATIONS") || p.hasPermission("LIST_ANY_APPLICATIONS"))
                .isPresent();
    }

    @Decision(value = IS_ALLOWED, method= "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal, HttpRequest request) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("CREATE_APPLICATION"))
                .isPresent();
    }
    @Decision(value = MALFORMED, method = "POST")
    public Problem validateApplicationCreateRequest(ApplicationCreateRequest createRequest, RestContext context) {
        Set<ConstraintViolation<ApplicationCreateRequest>> violations = validator.validate(createRequest);
        if (violations.isEmpty()) {
            context.putValue(converter.createFrom(createRequest, ApplicationCreateRequest.class));
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(value = MALFORMED, method = "GET")
    public Problem validateApplicationSearchParams(Parameters params, RestContext context) {
        ApplicationSearchParams applicationSearchParams = converter.createFrom(params, ApplicationSearchParams.class);
        Set<ConstraintViolation<ApplicationSearchParams>> violations = validator.validate(applicationSearchParams);
        if (violations.isEmpty()) {
            context.putValue(applicationSearchParams);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }


    @Decision(POST)
    public Application create(Application application, EntityManager em) {
        application.setWriteProtected(false);
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.persist(application));
        em.detach(application);
        return application;
    }

    @Decision(HANDLE_OK)
    public List<Application> handleOk(ApplicationSearchParams params, UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Application> query = cb.createQuery(Application.class);
        Root<Application> applicationRoot = query.from(Application.class);

        if (!principal.hasPermission("LIST_ANY_APPLICATIONS")) {
            Join<User, Group> userJoin = applicationRoot.join("realms")
                    .join("assignments")
                    .join("group")
                    .join("users");
            query.where(cb.equal(userJoin.get("id"), principal.getId()));
        }

        List<ResourceField> embedEntities = some(params.getEmbed(), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());
        EntityGraph<Application> applicationGraph = em.createEntityGraph(Application.class);
        applicationGraph.addAttributeNodes("name", "description", "passTo", "virtualPath", "topPage", "writeProtected");

        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("realms"))) {
            applicationGraph.addAttributeNodes("realms");
            Subgraph<Realm> realmsGraph = applicationGraph.addSubgraph("realms");
            realmsGraph.addAttributeNodes("name", "description", "url");
        }

        return em.createQuery(query)
                .setHint("javax.persistence.fetchgraph", applicationGraph)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }

}
