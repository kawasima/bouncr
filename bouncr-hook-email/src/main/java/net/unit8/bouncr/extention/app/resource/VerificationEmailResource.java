package net.unit8.bouncr.extention.app.resource;

import enkan.Env;
import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.entity.UserProfileField;
import net.unit8.bouncr.entity.UserProfileValue;
import net.unit8.bouncr.entity.UserProfileVerification;
import net.unit8.bouncr.hook.email.config.MailConfig;
import net.unit8.bouncr.hook.email.service.SendMailService;

import javax.inject.Inject;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class VerificationEmailResource {
    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Inject
    private MailConfig mailConfig;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    private Optional<UserProfileVerification> findMailVerification(EntityManager em, String account) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserProfileVerification> query = cb.createQuery(UserProfileVerification.class);
        Root<UserProfileVerification> root = query.from(UserProfileVerification.class);
        Join<UserProfileVerification, User> userRoot = root.join("user");
        Join<UserProfileField, User> userProfileFieldRoot = root.join("userProfileField");
        query.where(cb.equal(userRoot.get("account"), account),
                cb.equal(userProfileFieldRoot.get("jsonName"), "email"));

        return em.createQuery(query)
                .getResultStream()
                .findAny();
    }

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<User> query = builder.createQuery(User.class);
        Root<User> userRoot = query.from(User.class);
        userRoot.fetch("userProfileValues", JoinType.LEFT);
        query.where(builder.equal(userRoot.get("account"), params.get("account")));

        EntityGraph<User> userGraph = em.createEntityGraph(User.class);
        userGraph.addAttributeNodes("account", "userProfileValues");

        User user = em.createQuery(query)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setHint("javax.persistence.fetchgraph", userGraph)
                .getResultStream().findAny().orElse(null);

        if (user != null) {
            context.putValue(user);
        }
        return user != null;
    }

    @Decision(PROCESSABLE)
    public Problem processable(User user, EntityManager em, RestContext context) {
        UserProfileValue emailValue = findMailVerification(em, user.getAccount()).map(verification -> {
            context.putValue(verification);
            return verification;
        }).flatMap(verification -> user.getUserProfileValues()
                .stream()
                .filter(v -> Objects.equals(v.getUserProfileField().getJsonName(), "email"))
                .findAny()
                .filter(v -> Objects.nonNull(v.getValue()))
        ).orElse(null);

        return emailValue != null ? null : Problem.valueOf(422);
    }

    @Decision(PUT)
    public Void update(User user, UserProfileValue emailValue, UserProfileVerification emailVerification, EntityManager em) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("baseUrl", Env.getString("EMAIL_BASE_URL", "http://localhost:3000/bouncr/api"));
        variables.put("user", user);
        variables.put("email", emailValue.getValue());
        variables.put("code", emailVerification.getCode());

        final SendMailService sendMailService = new SendMailService(mailConfig);
        sendMailService.send(emailValue.getValue(), "Verification", variables);
        return null;
    }

}
