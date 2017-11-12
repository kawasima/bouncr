package net.unit8.bouncr.web.service;

import enkan.collection.Headers;
import enkan.collection.Multimap;
import enkan.component.doma2.DomaProvider;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.util.HttpResponseUtils;
import net.unit8.bouncr.authn.OneTimePasswordGenerator;
import net.unit8.bouncr.authz.UserPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.web.dao.*;
import net.unit8.bouncr.web.entity.*;
import net.unit8.bouncr.web.form.SignInForm;
import org.seasar.doma.jdbc.NoResultException;
import org.seasar.doma.jdbc.SelectOptions;
import org.seasar.doma.jdbc.UniqueConstraintException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.redirect;
import static enkan.util.ThreadingUtils.some;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.web.entity.ActionType.USER_FAILED_SIGNIN;
import static net.unit8.bouncr.web.entity.ActionType.USER_SIGNIN;
import static net.unit8.bouncr.web.service.SignInService.PasswordCredentialStatus.*;

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
                Optional.ofNullable(form.getAccount()).orElse("(anonymous)"),
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

    public String signIn(User user, HttpRequest request) {
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

        UserProfileFieldDao userProfileFieldDao = daoProvider.getDao(UserProfileFieldDao.class);

        Map<String, Object> profileMap = userProfileFieldDao.selectValuesByUserId(user.getId())
                .stream()
                .collect(Collectors.toMap(UserProfile::getJsonName, UserProfile::getValue));
        profileMap.put("email", user.getEmail());
        profileMap.put("name", user.getName());

        storeProvider.getStore(BOUNCR_TOKEN).write(token,
                new UserPrincipal(
                        user.getId(),
                        user.getAccount(),
                        profileMap,
                        getPermissionsByRealm(user, permissionDao))
        );
        return token;
    }

    public HttpResponse responseSignedIn(String token, HttpRequest request, String redirectUrl) {
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

    /**
     * Check whether the given otp key is valid.
     *
     * @param user the user entity
     * @param code otp key inputted by the user
     * @return true if otp key is valid or the user doesn't activate the 2-factor authentication
     */
    public boolean validateOtpKey(User user, String code) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        OtpKey otpKey = userDao.selectOtpKeyById(user.getId());
        if (otpKey == null) return true;

        Set<String> codeSet = new OneTimePasswordGenerator(30)
                .generateTotpSet(otpKey.getKey(), 5)
                .stream()
                .map(n -> String.format(Locale.US, "%06d", n))
                .collect(Collectors.toSet());

        return codeSet.contains(code);
    }

    public PasswordCredentialStatus validatePasswordCredentialAttributes(User user) {
        PasswordCredentialDao passwordCredentialDao = daoProvider.getDao(PasswordCredentialDao.class);
        PasswordCredential passwordCredential = passwordCredentialDao.selectById(user.getId());

        if (passwordCredential.getInitial()) {
            return INITIAL;
        }

        if (config.getPasswordPolicy().getExpires() != null) {
            Instant createdAt = passwordCredential.getCreatedAt().toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now()));
            return (createdAt.plus(config.getPasswordPolicy().getExpires()).isBefore(config.getClock().instant())) ?
                    EXPIRED : VALID;
        }

        return VALID;
    }

    private Map<Long, Set<String>> getPermissionsByRealm(User user, PermissionDao permissionDao) {
        return permissionDao
                .selectByUserId(user.getId())
                .stream()
                .collect(Collectors.groupingBy(PermissionWithRealm::getRealmId))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e ->
                        e.getValue().stream()
                                .map(PermissionWithRealm::getPermission)
                                .collect(Collectors.toSet())
                ));
    }

    public enum PasswordCredentialStatus {
        VALID,
        INITIAL,
        EXPIRED
    }
}
