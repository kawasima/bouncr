package net.unit8.bouncr.api.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import enkan.component.BeansConverter;
import enkan.util.BeanBuilder;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.SignUpCreateRequest;
import net.unit8.bouncr.api.service.UserProfileService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.entity.GroupInvitation;
import net.unit8.bouncr.entity.Invitation;
import net.unit8.bouncr.entity.OidcUser;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtClaim;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static kotowari.restful.DecisionPoint.MALFORMED;
import static kotowari.restful.DecisionPoint.POST;

@AllowedMethods({"POST"})
public class SignUpResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Inject
    private JsonWebToken jsonWebToken;
    @Inject
    private BouncrConfiguration config;

    @Decision(MALFORMED)
    public Problem validate(SignUpCreateRequest createRequest, EntityManager em) {
        Set<ConstraintViolation<SignUpCreateRequest>> violations = validator.validate(createRequest);
        Problem problem = Problem.fromViolations(violations);

        UserProfileService userProfileService = new UserProfileService(em);
        Set<Problem.Violation> profileViolations = userProfileService.validateUserProfile(createRequest.getUserProfiles());
        problem.getViolations().addAll(profileViolations);

        return problem.getViolations().isEmpty() ? null : problem;
    }

    public Invitation findInvitation(String code, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Invitation> query = cb.createQuery(Invitation.class);
        Root<Invitation> invitationRoot = query.from(Invitation.class);
        invitationRoot.fetch("groups", JoinType.LEFT);
        invitationRoot.fetch("oidcInvitations", JoinType.LEFT);
        query.where(cb.equal(invitationRoot.get("code"), code));

        return em.createQuery(query)
                .getResultStream()
                .findAny()
                .orElse(null);
    }

    @Decision(POST)
    public User create(SignUpCreateRequest createRequest, EntityManager em) {
        User user = converter.createFrom(createRequest, User.class);

        Optional<Invitation> invitation = Optional.ofNullable(createRequest.getCode())
                .map(code -> findInvitation(code, em));
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            invitation.ifPresent(invi -> {
                user.setGroups(invi.getGroupInvitations()
                        .stream()
                        .map(GroupInvitation::getGroup)
                        .collect(Collectors.toList()));
                invi.getOidcInvitations()
                        .stream()
                        .forEach(oidcInvitation -> {
                            OidcUser oidcUser = BeanBuilder.builder(new OidcUser())
                                    .set(OidcUser::setUser, user)
                                    .set(OidcUser::setOidcProvider, oidcInvitation.getOidcProvider())
                                    .set(OidcUser::setOidcSub, jsonWebToken.decodePayload(oidcInvitation.getOidcPayload(), new TypeReference<JwtClaim>() {}).getSub())
                                    .build();
                            em.persist(oidcUser);
                        });

            });
            invitation.ifPresent(em::remove);
            em.persist(user);
        });

        config.getHookRepo().runHook(HookPoint.AFTER_SIGNUP, user);
        return user;
    }
}
