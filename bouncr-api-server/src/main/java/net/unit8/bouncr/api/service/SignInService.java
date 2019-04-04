package net.unit8.bouncr.api.service;

import enkan.data.HttpRequest;
import net.unit8.bouncr.api.authn.OneTimePasswordGenerator;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.CacheStoreMode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.ThreadingUtils.some;
import static net.unit8.bouncr.api.service.SignInService.PasswordCredentialStatus.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.entity.ActionType.USER_FAILED_SIGNIN;
import static net.unit8.bouncr.entity.ActionType.USER_SIGNIN;

public class SignInService {
    private final BouncrConfiguration config;
    private final EntityManager em;
    private final StoreProvider storeProvider;

    private static final Logger LOG = LoggerFactory.getLogger(SignInService.class);

    public SignInService(EntityManager em, StoreProvider storeProvider, BouncrConfiguration config) {
        this.config = config;
        this.storeProvider = storeProvider;
        this.em = em;
    }

    public boolean validateOtpKey(OtpKey otpKey, String code) {
        if (otpKey == null) return true;

        return new OneTimePasswordGenerator(30)
                .generateTotpSet(otpKey.getKey(), 5)
                .stream()
                .map(n -> String.format(Locale.US, "%06d", n))
                .collect(Collectors.toSet())
                .contains(code);
    }

    public PasswordCredentialStatus validatePasswordCredentialAttributes(User user) {
        PasswordCredential passwordCredential = user.getPasswordCredential();
        if (passwordCredential.isInitial()) {
            return INITIAL;
        }

        if (config.getPasswordPolicy().getExpires() != null) {
            Instant createdAt = passwordCredential.getCreatedAt().toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now()));
            return (createdAt.plus(config.getPasswordPolicy().getExpires()).isBefore(config.getClock().instant())) ?
                    EXPIRED : VALID;
        }

        return VALID;
    }

    public String createToken() {
        return UUID.randomUUID().toString();
    }

    public UserSession createUserSession(HttpRequest request, User user, String token) {
        String userAgent = some(request.getHeaders().get("User-Agent"),
                ua -> ua.substring(0, Math.min(ua.length(), 255))).orElse("");
        UserSession userSession = builder(new UserSession())
                .set(UserSession::setToken, token)
                .set(UserSession::setUser, user)
                .set(UserSession::setRemoteAddress, request.getRemoteAddr())
                .set(UserSession::setUserAgent, userAgent)
                .set(UserSession::setCreatedAt, LocalDateTime.now())
                .build();
        HashMap<String, Object> profileMap = new HashMap<>(user.getUserProfileValues()
                .stream()
                .collect(Collectors.toMap(v -> v.getUserProfileField().getJsonName(), UserProfileValue::getValue)));
        profileMap.put("iss", "bouncr");
        profileMap.put("uid", Long.toString(user.getId()));
        profileMap.put("sub", user.getAccount());
        profileMap.put("permissionsByRealm", getPermissionsByRealm(user));
        LOG.debug("signIn profileMap = {}", profileMap);
        storeProvider.getStore(BOUNCR_TOKEN).write(token, profileMap);

        return userSession;
    }

    public Map<String, List<String>> getPermissionsByRealm(User user) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Assignment> assignmentCriteria = cb.createQuery(Assignment.class);
        Root<Assignment> assignmentRoot = assignmentCriteria.from(Assignment.class);
        Join<Group, Assignment> groupJoin = assignmentRoot.join("group");
        Join<User, Group> userJoin = groupJoin.join("users");
        assignmentRoot.fetch("role").fetch("permissions");
        assignmentCriteria.where(cb.equal(userJoin.get("id"), user.getId()));

        EntityGraph<Assignment> assignmentGraph = em.createEntityGraph(Assignment.class);
        assignmentGraph.addAttributeNodes("realm", "role");
        Subgraph<Role> roleGraph = assignmentGraph.addSubgraph("role");
        roleGraph.addAttributeNodes("permissions");
        Subgraph<Permission> permissionsGraph = roleGraph.addSubgraph("permissions");
        permissionsGraph.addAttributeNodes("name");

        return em.createQuery(assignmentCriteria)
                .setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .setHint("javax.persistence.fetchgraph", assignmentGraph)
                .getResultStream()
                .collect(Collectors.groupingBy(Assignment::getRealm))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getId().toString(),
                        e -> new ArrayList<>(e.getValue().stream()
                                .flatMap(v -> v.getRole().getPermissions().stream())
                                .map(Permission::getName)
                                .collect(Collectors.toSet()))));
    }



    public enum PasswordCredentialStatus {
        VALID,
        INITIAL,
        EXPIRED
    }
}
