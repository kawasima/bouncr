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
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.GroupCreateRequest;
import net.unit8.bouncr.api.boundary.GroupUpdateRequest;
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

@AllowedMethods({"GET", "PUT", "DELETE"})
public class GroupResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "PUT")
    public Problem vaidateCreateRequest(GroupCreateRequest updateRequest, RestContext context) {
        if (updateRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        Set<ConstraintViolation<GroupCreateRequest>> violations = validator.validate(updateRequest);
        if (violations.isEmpty()) {
            context.putValue(updateRequest);
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

    @Decision(value = ALLOWED, method= "PUT")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("group:update") || p.hasPermission("any_group:update"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "DELETE")
    public boolean isDeleteAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("group:delete") || p.hasPermission("any_group:update"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "PUT")
    public boolean isConflict(GroupUpdateRequest updateRequest, Parameters params, EntityManager em) {
        if (Objects.equals(updateRequest.getName(), params.get("name"))) {
            return false;
        }
        UniquenessCheckService<Group> uniquenessCheckService = new UniquenessCheckService<>(em);
        return !uniquenessCheckService.isUnique(Group.class, "nameLower",
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
        CriteriaQuery<Group> query = cb.createQuery(Group.class);
        Root<Group> groupRoot = query.from(Group.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(groupRoot.get("name"), params.get("name")));
        if ((request.getRequestMethod().equalsIgnoreCase("GET") && !principal.hasPermission("any_group:read"))
                || (request.getRequestMethod().equalsIgnoreCase("PUT") && !principal.hasPermission("any_group:update"))
                || (request.getRequestMethod().equalsIgnoreCase("DELETE") && !principal.hasPermission("any_group:delete"))) {
            Join<Group, User> userRoot = groupRoot.join("users");
            predicates.add(cb.equal(userRoot.get("id"), principal.getId()));
        }

        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }

        List<ResourceField> embedEntities = some(params.get("embed"), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());
        EntityGraph<Group> groupGraph = em.createEntityGraph(Group.class);
        groupGraph.addAttributeNodes("name", "description");

        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("users"))) {
            groupGraph.addAttributeNodes("users");
            groupGraph.addSubgraph("users")
                    .addAttributeNodes("account");
        }

        Group group = em.createQuery(query)
                .setHint("javax.persistence.fetchgraph", groupGraph)
                .getResultStream().findAny().orElse(null);
        if (group != null) {
            context.putValue(group);
        }
        return group != null;
    }

    @Decision(HANDLE_OK)
    public Group find(Group group, EntityManager em) {
        em.detach(group);
        return group;
    }

    @Decision(PUT)
    public Group update(GroupUpdateRequest updateRequest, Group group, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> converter.copy(updateRequest, group));
        em.detach(group);
        return group;
    }

    @Decision(DELETE)
    public Void delete(Group group, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.remove(group));
        return null;
    }
}
