package net.unit8.bouncr.api.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import enkan.component.BeansConverter;
import enkan.util.BeanBuilder;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.SignUpCreateRequest;
import net.unit8.bouncr.api.boundary.SignUpResponse;
import net.unit8.bouncr.api.service.PasswordCredentialService;
import net.unit8.bouncr.api.service.UserProfileService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.data.InitialPassword;
import net.unit8.bouncr.domain.VerificationTargetSet;
import net.unit8.bouncr.entity.*;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtClaim;

import jakarta.inject.Inject;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

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
    public Problem validate(SignUpCreateRequest createRequest, RestContext context, EntityManager em) {
        if (createRequest == null) {
            return Problem.valueOf(400, "request is empty", BouncrProblem.MALFORMED.problemUri());
        }
        Set<ConstraintViolation<SignUpCreateRequest>> violations = validator.validate(createRequest);
        if (!violations.isEmpty()) {
            return Problem.fromViolations(violations);
        }

        config.getHookRepo().runHook(HookPoint.BEFORE_VALIDATE_USER_PROFILES, createRequest.getUserProfiles());
        UserProfileService userProfileService = new UserProfileService(em);
        Set<Problem.Violation> profileViolations = userProfileService.validateUserProfile(createRequest.getUserProfiles());

        if (!profileViolations.isEmpty()) {
            return Problem.valueOf(400);
        }

        context.putValue(createRequest);
        return null;
    }

    @Decision(ALLOWED)
    public boolean isAllowedSignUp() {
        return config.isSignUpEnabled();
    }

    @Decision(HANDLE_FORBIDDEN)
    public Problem forbidden() {
        return Problem.valueOf(403, "Signing up is NOT allowed");
    }

    @Decision(CONFLICT)
    public boolean conflict(SignUpCreateRequest createRequest,
                            RestContext context,
                            EntityManager em) {
        UserProfileService userProfileService = new UserProfileService(em);

        Set<Problem.Violation> violations = userProfileService.validateAccountUniqueness(createRequest.getAccount());
        violations.addAll(userProfileService.validateProfileUniqueness(createRequest.getUserProfiles()));

        if (!violations.isEmpty()) {
            context.setMessage(Problem.valueOf(409));
        }
        return !violations.isEmpty();
    }

    private Invitation findInvitation(String code, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Invitation> query = cb.createQuery(Invitation.class);
        Root<Invitation> invitationRoot = query.from(Invitation.class);
        invitationRoot.fetch("groupInvitations", JoinType.LEFT);
        invitationRoot.fetch("oidcInvitations", JoinType.LEFT);
        query.where(cb.equal(invitationRoot.get("code"), code));

        return em.createQuery(query)
                .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream()
                .findAny()
                .orElse(null);
    }

    @Decision(POST)
    public SignUpResponse create(SignUpCreateRequest createRequest, RestContext context, EntityManager em) {
        User user = converter.createFrom(createRequest, User.class);
        UserProfileService userProfileService = new UserProfileService(em);
        // Process user profiles
        List<UserProfileValue> userProfileValues = userProfileService
                .convertToUserProfileValues(createRequest.getUserProfiles());
        user.setUserProfileValues(userProfileValues.stream()
                .map(v -> { v.setUser(user); return v; })
                .collect(Collectors.toList()));
        // Process user profile verifications
        List<UserProfileVerification> profileVerifications = userProfileService
                .createProfileVerification(userProfileValues).stream()
                .map(v -> {
                    v.setUser(user);
                    return v;
                })
                .collect(Collectors.toList());

        user.setWriteProtected(false);
        context.putValue(user);

        config.getHookRepo().runHook(HookPoint.BEFORE_SIGN_UP, context);

        Optional<Invitation> invitation = Optional.ofNullable(createRequest.getCode())
                .map(code -> findInvitation(code, em));
        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> {
            invitation.ifPresent(invi -> {
                List<Group> groups = Optional.ofNullable(user.getGroups())
                        .filter(gs -> !gs.isEmpty())
                        .orElseGet(ArrayList::new);
                groups.addAll(invi.getGroupInvitations()
                        .stream()
                        .map(GroupInvitation::getGroup)
                        .collect(Collectors.toList()));
                user.setGroups(groups);
                invi.getOidcInvitations()
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
            context.putValue(user);
            profileVerifications.forEach(em::persist);
            context.putValue(new VerificationTargetSet(profileVerifications));
            if (createRequest.isEnablePasswordCredential()) {
                PasswordCredentialService passwordCredentialService = new PasswordCredentialService(em, config);
                InitialPassword initialPassword = passwordCredentialService.initializePassword(user);
                context.putValue(initialPassword);
            }
            config.getHookRepo().runHook(HookPoint.AFTER_SIGN_UP, context);
        });

        return builder(new SignUpResponse())
                .set(SignUpResponse::setId, user.getId())
                .set(SignUpResponse::setAccount, user.getAccount())
                .set(SignUpResponse::setUserProfiles, user.getUserProfiles())
                .set(SignUpResponse::setPassword, context.getValue(InitialPassword.class)
                        .map(InitialPassword::getPassword)
                        .orElse(null))
                .build();
    }
}
