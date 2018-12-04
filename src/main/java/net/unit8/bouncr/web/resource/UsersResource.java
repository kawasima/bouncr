package net.unit8.bouncr.web.resource;

import enkan.component.BeansConverter;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.apistandard.resourcefilter.ResourceField;
import net.unit8.apistandard.resourcefilter.ResourceFilter;
import net.unit8.bouncr.web.boundary.CreateUserRequest;
import net.unit8.bouncr.web.boundary.UserSearchParam;
import net.unit8.bouncr.web.entity.Group;
import net.unit8.bouncr.web.entity.User;
import org.eclipse.persistence.indirection.IndirectList;

import javax.inject.Inject;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class UsersResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateUserCreateRequest(CreateUserRequest createRequest, RestContext context) {
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(createRequest);
        if (violations.isEmpty()) {
            context.putValue(converter.createFrom(createRequest, User.class));
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(HANDLE_OK)
    public List<User> handleOk(UserSearchParam params, EntityManager em) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<User> query = builder.createQuery(User.class);
        Root<User> user = query.from(User.class);

        List<ResourceField> embedEntities = some(params.getEmbed(), embed -> new ResourceFilter().parse(embed))
                .orElse(Collections.emptyList());
        EntityGraph<User> userGraph = em.createEntityGraph(User.class);
        userGraph.addAttributeNodes("name", "account", "email", "writeProtected");

        if (params.getGroupId() != null) {
            Join<Group, User> groups = user.join("groups");
            query.where(builder.equal(groups.get("id"), params.getGroupId()));
        }
        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("groups"))) {
            userGraph.addAttributeNodes("groups");
            //userGraph.addSubgraph("groups");
        }
        return em.createQuery(query)
                .setHint("javax.persistence.fetchgraph", userGraph)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList();
    }

    @Decision(POST)
    public User doPost(User user, EntityManager em) {
        EntityTransactionManager tm = new EntityTransactionManager(em);
        tm.required(() -> em.persist(user));
        return user;
    }
}
