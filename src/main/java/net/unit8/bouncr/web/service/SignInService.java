package net.unit8.bouncr.web.service;

import enkan.collection.Headers;
import enkan.collection.Multimap;
import enkan.component.doma2.DomaProvider;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import static enkan.util.BeanBuilder.builder;
import enkan.util.HttpResponseUtils;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.web.dao.AuditDao;
import net.unit8.bouncr.web.dao.PermissionDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.dao.UserSessionDao;
import net.unit8.bouncr.web.entity.PermissionWithRealm;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.entity.UserSession;
import net.unit8.bouncr.web.form.SignInForm;
import org.seasar.doma.jdbc.NoResultException;
import org.seasar.doma.jdbc.SelectOptions;
import org.seasar.doma.jdbc.UniqueConstraintException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.HttpResponseUtils.redirect;
import static enkan.util.ThreadingUtils.some;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.web.entity.ActionType.USER_FAILED_SIGNIN;
import static net.unit8.bouncr.web.entity.ActionType.USER_SIGNIN;

public class SignInService {
    private final DomaProvider daoProvider;
    private final StoreProvider storeProvider;
    private final BouncrConfiguration config;

    public SignInService(DomaProvider daoProvider, StoreProvider storeProvider, BouncrConfiguration config) {
        this.daoProvider = daoProvider;
        this.storeProvider = storeProvider;
        this.config = config;
    }

    /**
     * Record the event of signing in
     *
     * @param user    the user entity
     * @param request the http request
     * @param form    the form for sign in
     */
    public void recordSignIn(User user, HttpRequest request, SignInForm form) {
        AuditDao auditDao = daoProvider.getDao(AuditDao.class);
        auditDao.insertUserAction(user != null ? USER_SIGNIN : USER_FAILED_SIGNIN,
                form.getAccount(),
                request.getRemoteAddr());

        if (user == null) {
            UserDao userDao = daoProvider.getDao(UserDao.class);
            try {
                user = userDao.selectByAccount(form.getAccount());
            } catch(NoResultException ignore) {}
            if (user != null) {
                List<String> actionTypes = auditDao.selectRecentSigninHistories(form.getAccount(),
                        SelectOptions.get().limit(config.getPasswordPolicy().getNumOfTrialsUntilLock()));
                if (actionTypes.stream().filter(t -> t.equals("user.failed_signin")).count() >= config.getPasswordPolicy().getNumOfTrialsUntilLock()) {
                    try {
                        userDao.lock(user.getId());
                    } catch(UniqueConstraintException ignore) {

                    }
                }
            }
        }
    }

    public HttpResponse signIn(User user, HttpRequest request, String redirectUrl) {
        PermissionDao permissionDao = daoProvider.getDao(PermissionDao.class);
        String token = UUID.randomUUID().toString();

        UserSessionDao userSessionDao = daoProvider.getDao(UserSessionDao.class);

        String userAgent = some(request.getHeaders().get("User-Agent"),
                ua -> ua.substring(0, Math.min(ua.length(), 255))).orElse("");
        userSessionDao.insert(builder(new UserSession())
                .set(UserSession::setToken, token)
                .set(UserSession::setUserId, user.getId())
                .set(UserSession::setRemoteAddress, request.getRemoteAddr())
                .set(UserSession::setUserAgent, userAgent)
                .set(UserSession::setCreatedAt, LocalDateTime.now())
                .build());

        storeProvider.getStore(BOUNCR_TOKEN).write(token, new HashMap<>(getPermissionsByRealm(user, permissionDao)));

        Cookie tokenCookie = Cookie.create(config.getTokenName(), token);
        tokenCookie.setPath("/");
        tokenCookie.setHttpOnly(true);

        if (Objects.equals(request.getHeaders().get("X-Requested-With"), "XMLHttpRequest")) {
            Multimap<String, Cookie> cookies = Multimap.of(tokenCookie.getName(), tokenCookie);
            return builder(HttpResponse.of("{\"url\":\"" + Optional.ofNullable(redirectUrl).orElse("/my") + "\"}"))
                    .set(HttpResponse::setHeaders, Headers.of("Content-Type", "application/json"))
                    .set(HttpResponse::setCookies, cookies)
                    .build();
        } else {
            return builder(redirect(Optional.ofNullable(redirectUrl).orElse("/my"),
                    HttpResponseUtils.RedirectStatusCode.SEE_OTHER))
                    .set(HttpResponse::setCookies, Multimap.of(tokenCookie.getName(), tokenCookie))
                    .build();
        }
    }

    private Map<Long, UserPermissionPrincipal> getPermissionsByRealm(User user, PermissionDao permissionDao) {
        return permissionDao
                .selectByUserId(user.getId())
                .stream()
                .collect(Collectors.groupingBy(PermissionWithRealm::getRealmId))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e ->
                        new UserPermissionPrincipal(
                                user.getId(),
                                user.getAccount(),
                                user.getEmail(),
                                e.getValue().stream()
                                        .map(PermissionWithRealm::getPermission)
                                        .collect(Collectors.toSet()))));
    }
}
