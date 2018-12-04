package net.unit8.bouncr.web.resource;

import enkan.data.HttpRequest;
import kotowari.restful.Decision;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.web.boundary.PasswordSignInRequest;
import net.unit8.bouncr.web.entity.PasswordCredential;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.entity.UserSession;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import java.time.LocalDateTime;
import java.util.UUID;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.IS_AUTHORIZED;
import static kotowari.restful.DecisionPoint.POST;

public class PasswordSignInResource {
    @Decision(IS_AUTHORIZED)
    public Problem authenticate(PasswordSignInRequest passwordSignInRequest, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PasswordCredential> query = cb.createQuery(PasswordCredential.class);
        Root<PasswordCredential> root = query.from(PasswordCredential.class);
        query.where(cb.equal(root.get("account"), passwordSignInRequest.getAccount()));
        PasswordCredential passwordCredential = em.createQuery(query).getSingleResult();

        if (passwordCredential != null) {
            return null;
        } else {
            return new Problem(null, "Authentication failed", 401, null, null);
        }
    }

    @Decision(POST)
    public UserSession doPost(User user, HttpRequest request, EntityManager em) {
        String token = UUID.randomUUID().toString();

        String userAgent = some(request.getHeaders().get("User-Agent"),
                ua -> ua.substring(0, Math.min(ua.length(), 255))).orElse("");
        UserSession userSession = builder(new UserSession())
                .set(UserSession::setToken, token)
                .set(UserSession::setUserId, user.getId())
                .set(UserSession::setRemoteAddress, request.getRemoteAddr())
                .set(UserSession::setUserAgent, userAgent)
                .set(UserSession::setCreatedAt, LocalDateTime.now())
                .build();
        em.persist(userSession);
        return userSession;
    }
}
