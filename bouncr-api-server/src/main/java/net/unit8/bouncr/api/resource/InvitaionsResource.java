package net.unit8.bouncr.api.resource;

import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.InvitationCreateRequest;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.Invitation;
import net.unit8.bouncr.util.RandomUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.validation.ConstraintViolation;

import java.util.Optional;
import java.util.Set;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class InvitaionsResource {
    @Inject
    private BouncrConfiguration config;

    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(value = MALFORMED, method = "POST")
    public Problem vaidateCreateRequest(InvitationCreateRequest createRequest, RestContext context) {
        Set<ConstraintViolation<InvitationCreateRequest>> violations = validator.validate(createRequest);
        if (violations.isEmpty()) {
            context.putValue(createRequest);
        }
        return violations.isEmpty() ? null : Problem.fromViolations(violations);
    }

    @Decision(IS_AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = IS_ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("CREATE_INVITATION"))
                .isPresent();
    }

    @Decision(POST)
    public Invitation create(InvitationCreateRequest createRequest, EntityManager em) {
        String code = RandomUtils.generateRandomString(8, config.getSecureRandom());
        Invitation invitation = converter.createFrom(createRequest, Invitation.class);
        invitation.setCode(code);

        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.persist(invitation));
        return invitation;
    }

}
