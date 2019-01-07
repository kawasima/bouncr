package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.apistandard.resourcefilter.ResourceField;
import net.unit8.apistandard.resourcefilter.ResourceFilter;
import net.unit8.bouncr.api.boundary.GroupCreateRequest;
import net.unit8.bouncr.api.boundary.GroupSearchParams;
import net.unit8.bouncr.entity.Group;

import javax.inject.Inject;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class GroupsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("LIST_GROUPS") || p.hasPermission("LIST_ANY_GROUPS"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method= "POST")
    public boolean isPutAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("CREATE_GROUP") || p.hasPermission("CREATE_ANY_GROUP"))
                .isPresent();
    }

    @Decision(value = MALFORMED, method = "GET")
    public Problem validateSearchParams(Parameters params, RestContext context) {
        GroupSearchParams groupSearchParams = converter.createFrom(params, GroupSearchParams.class);
        Set<ConstraintViolation<GroupSearchParams>> violations = validator.validate(groupSearchParams);
        if (violations.isEmpty()) {
            context.putValue(groupSearchParams);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem vaidateCreateRequest(GroupCreateRequest createRequest, RestContext context) {
        Set<ConstraintViolation<GroupCreateRequest>> violations = validator.validate(createRequest);
        if (violations.isEmpty()) {
            context.putValue(createRequest);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(HANDLE_OK)
    public List<Group> handleOk(GroupSearchParams params, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> query = cb.createQuery(Group.class);
        Root<Group> groupRoot = query.from(Group.class);
        query.orderBy(cb.asc(groupRoot.get("id")));

        List<ResourceField> embedEntities = some(params.getEmbed(), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());
        EntityGraph<Group> groupGraph = em.createEntityGraph(Group.class);
        groupGraph.addAttributeNodes("name", "description");

        if (params.getQ() != null) {
            query.where(cb.like(groupRoot.get("name"), '%' + params.getQ() + '%'));
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
