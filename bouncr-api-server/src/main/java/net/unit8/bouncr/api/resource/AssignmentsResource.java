package net.unit8.bouncr.api.resource;

import enkan.component.BeansConverter;
import enkan.exception.UnreachableException;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.AssignmentRequest;
import net.unit8.bouncr.api.boundary.AssignmentsRequest;
import net.unit8.bouncr.entity.Assignment;
import net.unit8.bouncr.entity.Group;
import net.unit8.bouncr.entity.Realm;
import net.unit8.bouncr.entity.Role;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.boundary.AssignmentRequest.IdObject;

/**
 * Assignments Resource.
 *
 * This resource has
 *
 * <ul>
 *   <li>POST: Create multiple assignments</li>
 *   <li>DELETE: Create multiple assignments</li>
 * </ul>
 *
 * @author kawasima
 */
@AllowedMethods({"POST", "DELETE"})
public class AssignmentsResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    private Problem.Violation validateIdObject(IdObject obj, String type, int index) {
        return (obj == null || (obj.getId() == null && obj.getName() == null)) ?
                new Problem.Violation("assignments[" + index + "][" + type + "]", "must be null")
                :
                null;

    }

    private static final Map<String, Function<AssignmentRequest, AssignmentRequest.IdObject>> ASSIGNMENT_ELEMENT_TYPES  =Map.of(
            "group", AssignmentRequest::getGroup,
            "role",  AssignmentRequest::getRole,
            "realm", AssignmentRequest::getRealm
            );

    private Assignment findAssignment(Group group, Role role, Realm realm, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Assignment> query = cb.createQuery(Assignment.class);
        Root<Assignment> root = query.from(Assignment.class);
        query.where(
                cb.equal(root.get("group"), group),
                cb.equal(root.get("role"),  role),
                cb.equal(root.get("realm"), realm)
                );
        return em.createQuery(query).getResultStream().findAny().orElse(null);
    }
    private <T> T findAssignmentElement(Class<T> entityClass, AssignmentRequest.IdObject idObject, EntityManager em) {
        if (idObject.getId() != null) {
            return em.find(entityClass, idObject.getId());
        } else if (idObject.getName() != null) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<T> query = cb.createQuery(entityClass);
            Root<T> root = query.from(entityClass);
            query.where(cb.equal(root.get("name"), idObject.getName()));
            return em.createQuery(query).getResultStream().findAny().orElse(null);
        } else {
            throw new UnreachableException();
        }
    }

    private Problem validateInner(TriFunction<Group, Role, Realm, Assignment> createAssignmentFunc,
                                  AssignmentsRequest assignmentsRequest,
                                  RestContext context,
                                  EntityManager em) {
        List<Problem.Violation> violations = IntStream.range(0, assignmentsRequest.size())
                .mapToObj(i -> {
                    AssignmentRequest r = assignmentsRequest.get(i);
                    return ASSIGNMENT_ELEMENT_TYPES.entrySet()
                            .stream()
                            .map(e -> {
                                IdObject idObj = e.getValue().apply(r);
                                return (idObj == null || (idObj.getId() == null && idObj.getName() == null)) ?
                                        new Problem.Violation("assignments[" + i + "][" + e.getKey() + "]", "must be null")
                                        :
                                        null;
                            })
                            .filter(Objects::nonNull)
                            .findAny()
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Assignments assignments = new Assignments();
        if (violations.isEmpty()) {
            for (int i=0; i<assignmentsRequest.size(); i++) {
                AssignmentRequest r = assignmentsRequest.get(i);
                Group group = findAssignmentElement(Group.class, r.getGroup(), em);
                Role  role  = findAssignmentElement(Role.class, r.getRole(), em);
                Realm realm = findAssignmentElement(Realm.class, r.getRealm(), em);

                if (group == null) {
                    violations.add(new Problem.Violation("assignments[" + i + "][group]", "not found"));
                    continue;
                }
                if (role == null) {
                    violations.add(new Problem.Violation("assignments[" + i + "][role]", "not found"));
                    continue;
                }
                if (realm == null) {
                    violations.add(new Problem.Violation("assignments[" + i + "][realm]", "not found"));
                    continue;
                }
                Assignment assignment = createAssignmentFunc.apply(group, role, realm);
                if (assignment != null) {
                    assignments.add(assignment);
                }
            }
        }

        if (!violations.isEmpty()) {
            Problem problem = new Problem(URI.create("about:blank"),
                    "Malformed assignmentsRequest",
                    400,
                    "",
                    null);
            problem.getViolations().addAll(violations);
            return problem;
        }

        context.putValue(assignments);
        return null;
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateForPost(AssignmentsRequest assignmentsRequest, RestContext context, EntityManager em) {
        return validateInner((group, role, realm) -> builder(new Assignment())
                .set(Assignment::setGroup, group)
                .set(Assignment::setRole,  role)
                .set(Assignment::setRealm, realm)
                .build(),
                assignmentsRequest, context, em);
    }

    @Decision(value = MALFORMED, method = "DELETE")
    public Problem validateForDelete(AssignmentsRequest assignmentsRequest, RestContext context, EntityManager em) {
        return validateInner((group, role, realm) -> findAssignment(group, role, realm, em),
                assignmentsRequest, context, em);
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }


    public static class Assignments extends ArrayList<Assignment>{}

    @Decision(POST)
    public Void create(Assignments assignments, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> assignments
                .forEach(em::persist));
        return null;
    }

    @Decision(DELETE)
    public Void delete(Assignments assignments, EntityManager em) {
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> assignments
                .forEach(em::remove));
        return null;
    }

    @FunctionalInterface
    interface TriFunction<A,B,C,R> {
        R apply(A a, B b, C c);

        default <V> TriFunction<A, B, C, V> andThen(
                Function<? super R, ? extends V> after) {
            Objects.requireNonNull(after);
            return (A a, B b, C c) -> after.apply(apply(a, b, c));
        }
    }
}
