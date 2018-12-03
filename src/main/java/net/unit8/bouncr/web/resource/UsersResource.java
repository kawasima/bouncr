package net.unit8.bouncr.web.resource;

import enkan.component.BeansConverter;
import enkan.data.DefaultHttpRequest;
import enkan.util.Predicates;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.DefaultResouruce;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.sf.ehcache.pool.sizeof.filter.ResourceSizeOfFilter;
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
import static kotowari.restful.DecisionPoint.HANDLE_OK;
import static kotowari.restful.DecisionPoint.MALFORMED;
import static kotowari.restful.DecisionPoint.POST;

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
        Predicate criteria = builder.conjunction();
        if (params.getGroupId() != null) {
            Subquery<Group> sq = query.subquery(Group.class);
            Root<Group> group = sq.from(Group.class);
            Join<User, Group> sqUser = group.join("users");
            sq.select(group)
                    .where(
                            builder.equal(sqUser, user),
                            builder.equal(group.get("id"), params.getGroupId())
                    );
            criteria = builder.and(criteria, builder.exists(sq));
        }
        if (embedEntities.stream().anyMatch(r -> r.getName().equalsIgnoreCase("groups"))) {
            query.distinct(true);
            user.fetch("groups");
        }
        query.where(criteria);
        return em.createQuery(query)
                .setFirstResult(params.getOffset())
                .setMaxResults(params.getLimit())
                .getResultList()
                .stream()
                .map(u -> {
                    if (!((IndirectList)u.getGroups()).isInstantiated()) {
                        u.setGroups(null);
                    }
                    return u;
                })
                .collect(Collectors.toList());
    }

    @Decision(POST)
    public User doPost(User user, EntityManager em) {
        EntityTransactionManager tm = new EntityTransactionManager(em);
        tm.required(() -> em.persist(user));
        return user;
    }
}
