package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.apistandard.resourcefilter.ResourceField;
import net.unit8.apistandard.resourcefilter.ResourceFilter;
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
import java.util.Set;

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
        GroupSearchParams userSearchParam = converter.createFrom(params, GroupSearchParams.class);
        Set<ConstraintViolation<GroupSearchParams>> violations = validator.validate(userSearchParam);
        if (violations.isEmpty()) {
            context.putValue(userSearchParam);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);

    }

    @Decision(HANDLE_OK)
    public List<Group> handleOk(GroupSearchParams params, EntityManager em) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Group> query = builder.createQuery(Group.class);
        Root<Group> groupRoot = query.from(Group.class);

        List<ResourceField> embedEntities = some(params.getEmbed(), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());
        EntityGraph<Group> groupGraph = em.createEntityGraph(Group.class);
        groupGraph.addAttributeNodes("name", "description", "writeProtected");

        if (params.getQ() != null) {
            query.where(builder.like(groupRoot.get("name"), '%' + params.getQ() + '%'));
        }
        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("users"))) {
            groupGraph.addAttributeNodes("users");
        }
        return em.createQuery(query)
                .setHint("javax.persistence.fetchgraph", groupGraph)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }

    @Decision(POST)
    public Group doPost(Group group, EntityManager em) {
        EntityTransactionManager tm = new EntityTransactionManager(em);
        tm.required(() -> em.persist(group));
        return group;
    }
}
