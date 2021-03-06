package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
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
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.GroupCreateRequest;
import net.unit8.bouncr.api.boundary.GroupSearchParams;
import net.unit8.bouncr.api.service.UniquenessCheckService;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.User;

import javax.inject.Inject;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import javax.validation.ConstraintViolation;
import java.util.*;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class GroupsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "GET")
    public Problem validateSearchParams(Parameters params, RestContext context) {
        GroupSearchParams groupSearchParams = converter.createFrom(params, GroupSearchParams.class);
        Set<ConstraintViolation<GroupSearchParams>> violations = validator.validate(groupSearchParams);
        if (violations.isEmpty()) {
            context.putValue(groupSearchParams);
        }
        return violations.isEmpty() ? null : builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem vaidateCreateRequest(GroupCreateRequest createRequest, RestContext context) {
        if (createRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        Set<ConstraintViolation<GroupCreateRequest>> violations = validator.validate(createRequest);
        if (violations.isEmpty()) {
            context.putValue(createRequest);
        }
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
                .filter(p -> p.hasPermission("group:read") || p.hasPermission("any_group:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "POST")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("group:create") || p.hasPermission("any_group:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(GroupCreateRequest createRequest, EntityManager em) {
        UniquenessCheckService<Group> uniquenessCheckService = new UniquenessCheckService<>(em);
        return !uniquenessCheckService.isUnique(Group.class, "nameLower",
                Optional.ofNullable(createRequest.getName())
                        .map(n -> n.toLowerCase(Locale.US))
                        .orElseThrow(UnreachableException::new));
    }

    @Decision(HANDLE_OK)
    public List<Group> handleOk(GroupSearchParams params, UserPermissionPrincipal principal, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> query = cb.createQuery(Group.class);
        Root<Group> groupRoot = query.from(Group.class);
        query.distinct(true);
        query.orderBy(cb.asc(groupRoot.get("id")));

        List<ResourceField> embedEntities = some(params.getEmbed(), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());
        EntityGraph<Group> groupGraph = em.createEntityGraph(Group.class);
        groupGraph.addAttributeNodes("name", "description");

        List<Predicate> predicates = new ArrayList<>();
        if (!principal.hasPermission("any_group:read")) {
            Join<User, Group> userJoin = groupRoot.join("users");
            predicates.add(cb.equal(userJoin.get("id"), principal.getId()));
        }

        Optional.ofNullable(params.getQ())
                .ifPresent(q -> {
                    String likeExpr = "%" + q.replaceAll("%", "_%") + "%";
                    predicates.add(cb.like(groupRoot.get("name"), likeExpr, '_'));
                });
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }


        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("users"))) {
            groupGraph.addAttributeNodes("users");
            groupGraph.addSubgraph("users")
                    .addAttributeNodes("account");
        }
        return em.createQuery(query)
                .setHint("javax.persistence.fetchgraph", groupGraph)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }

    @Decision(POST)
    public Group doPost(GroupCreateRequest createRequest, EntityManager em) {
        Group group = converter.createFrom(createRequest, Group.class);
        group.setWriteProtected(false);
        EntityTransactionManager tm = new EntityTransactionManager(em);
        tm.required(() -> em.persist(group));
        return group;
    }
}
