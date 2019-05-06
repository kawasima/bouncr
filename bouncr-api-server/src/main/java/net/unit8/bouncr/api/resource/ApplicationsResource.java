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
import net.unit8.apistandard.resourcefilter.ResourceField;
import net.unit8.apistandard.resourcefilter.ResourceFilter;
import net.unit8.bouncr.api.boundary.ApplicationCreateRequest;
import net.unit8.bouncr.api.boundary.ApplicationSearchParams;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.service.UniquenessCheckService;
import net.unit8.bouncr.entity.Application;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.Realm;
import net.unit8.bouncr.entity.User;

import javax.inject.Inject;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;
import javax.persistence.criteria.*;
import javax.validation.ConstraintViolation;
import java.util.*;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class ApplicationsResource {
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
                .filter(p -> p.hasPermission("application:read") || p.hasPermission("any_application:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal, HttpRequest request) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("any_application:create"))
                .isPresent();
    }
    @Decision(value = MALFORMED, method = "POST")
    public Problem validateApplicationCreateRequest(ApplicationCreateRequest createRequest, RestContext context) {
        if (createRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        Set<ConstraintViolation<ApplicationCreateRequest>> violations = validator.validate(createRequest);
        return violations.isEmpty() ? null : builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
    }

    @Decision(value = MALFORMED, method = "GET")
    public Problem validateApplicationSearchParams(Parameters params, RestContext context) {
        ApplicationSearchParams applicationSearchParams = converter.createFrom(params, ApplicationSearchParams.class);
        Set<ConstraintViolation<ApplicationSearchParams>> violations = validator.validate(applicationSearchParams);
        if (violations.isEmpty()) {
            context.putValue(applicationSearchParams);
        }
        return violations.isEmpty() ? null : builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(ApplicationCreateRequest createRequest, EntityManager em) {
        UniquenessCheckService<Application> uniquenessCheckService = new UniquenessCheckService<>(em);
        return !uniquenessCheckService.isUnique(Application.class, "nameLower",
                Optional.ofNullable(createRequest.getName())
                        .map(n -> n.toLowerCase(Locale.US))
                        .orElseThrow(UnreachableException::new));
    }

    @Decision(POST)
    public Application create(ApplicationCreateRequest createRequest, EntityManager em) {
        Application application  = converter.createFrom(createRequest, Application.class);
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
        query.distinct(true);

        List<Predicate> predicates = new ArrayList<>();
        if (!principal.hasPermission("any_application:read")) {
            Join<User, Group> userJoin = applicationRoot.join("realms")
                    .join("assignments")
                    .join("group")
                    .join("users");
            predicates.add(cb.equal(userJoin.get("id"), principal.getId()));
        }

        Optional.ofNullable(params.getQ())
                .ifPresent(q -> {
                    String likeExpr = "%" + q.replaceAll("%", "_%") + "%";
                    predicates.add(cb.like(applicationRoot.get("name"), likeExpr, '_'));
                });
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
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
