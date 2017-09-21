package net.unit8.bouncr.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import enkan.collection.Multimap;
import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.exception.FalteringEnvironmentException;
import enkan.util.BeanBuilder;
import enkan.util.CodecUtils;
import enkan.util.HttpResponseUtils;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import lombok.Data;
import net.unit8.bouncr.authn.OneTimePasswordGenerator;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.LdapClient;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.sign.IdToken;
import net.unit8.bouncr.sign.IdTokenPayload;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.controller.data.OidcSession;
import net.unit8.bouncr.web.dao.*;
import net.unit8.bouncr.web.entity.*;
import net.unit8.bouncr.web.form.SignInForm;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.seasar.doma.jdbc.NoResultException;
import org.seasar.doma.jdbc.SelectOptions;
import org.seasar.doma.jdbc.UniqueConstraintException;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;
import static enkan.util.HttpResponseUtils.redirect;
import static enkan.util.ThreadingUtils.some;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.component.StoreProvider.StoreType.OIDC_SESSION;
import static net.unit8.bouncr.web.entity.ActionType.USER_FAILED_SIGNIN;
import static net.unit8.bouncr.web.entity.ActionType.USER_SIGNIN;

public class SignInController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private LdapClient ldapClient;

    @Inject
    private IdToken idToken;

    @Inject
    private BouncrConfiguration config;

    public HttpResponse signInForm(HttpRequest request, SignInForm form) {
        String account = getAccountFromClientDN(request);
        if (account != null) {
            form.setAccount(account);
            return templateEngine.render("my/signIn/clientdn",
                    "signin", form);
        } else {
            OidcProviderDao oauth2ProviderDao = daoProvider.getDao(OidcProviderDao.class);
            String oidcSessionId = UUID.randomUUID().toString();
            OidcSession oidcSession = OidcSession.create(config.getSecureRandom());

            List<OAuth2ProviderDto> oauth2Providers = oauth2ProviderDao
                    .selectAll()
                    .stream()
                    .map(p -> {
                        OAuth2ProviderDto dto = beansConverter.createFrom(p, OAuth2ProviderDto.class);
                        dto.setNonce(oidcSession.getNonce());
                        dto.setState(oidcSession.getState());
                        dto.setRedirectUriBase(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort());
                        return dto;
                    })
                    .collect(Collectors.toList());

            storeProvider.getStore(OIDC_SESSION).write(oidcSessionId, oidcSession);
            Cookie cookie = builder(Cookie.create("OIDC_SESSION_ID", oidcSessionId))
                    .set(Cookie::setPath, "/")
                    .build();
            return builder(templateEngine.render("my/signIn/index",
                    "oauth2Providers", oauth2Providers,
                    "signin", form))
                    .set(HttpResponse::setCookies, Multimap.of("OIDC_SESSION_ID", cookie))
                    .build();
        }
    }

    @Data
    public static class OAuth2ProviderDto implements Serializable {
        private Long id;
        private String name;
        private String apiKey;
        private String scope;
        private String state = RandomUtils.generateRandomString(8);
        private String responseType;
        private String accessTokenEndpoint;
        private String authorizationBaseUrl;
        private String nonce = RandomUtils.generateRandomString(32);
        private String redirectUriBase;

        public String getRedirectUri() {
            return redirectUriBase
                    + "/my/signIn/oauth/" + id;
        }

        public String getAuthorizationUrl() {
            return authorizationBaseUrl + "?response_type=" + CodecUtils.urlEncode(responseType)
                    + "&client_id=" + apiKey
                    + "&redirect_uri=" + CodecUtils.urlEncode(getRedirectUri())
                    + "&state=" + state
                    + "&scope=" + scope
                    + "&nonce=" + nonce;
        }
    }

    /**
     * Record the event of signing in
     *
     * @param user    the user entity
     * @param request the http request
     * @param form    the form for sign in
     */
    private void recordSignIn(User user, HttpRequest request, SignInForm form) {
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
    private HttpResponse signIn(User user, HttpRequest request, String redirectUrl) {
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
        return BeanBuilder.builder(redirect(Optional.ofNullable(redirectUrl).orElse("/my"),
                HttpResponseUtils.RedirectStatusCode.SEE_OTHER))
                .set(HttpResponse::setCookies, Multimap.of(tokenCookie.getName(), tokenCookie))
                .build();
    }

    @Transactional
    public HttpResponse signInByPassword(HttpRequest request, SignInForm form) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        if (userDao.isLock(form.getAccount())) {
            form.setErrors(Multimap.of("account", "error.accountLocked"));
            return signInForm(request, form);
        }
        User user= userDao.selectByPassword(form.getAccount(), form.getPassword());

        if (user == null && ldapClient != null) {
            if (ldapClient.search(form.getAccount(), form.getPassword())) {
                user = userDao.selectByAccount(form.getAccount());
            }
        }

        if (user != null) {
            OtpKey otpKey = userDao.selectOtpKeyById(user.getId());
            if (otpKey != null) {
                Set<String> codeSet = new OneTimePasswordGenerator(30)
                        .generateTotpSet(otpKey.getKey(), 5)
                        .stream()
                        .map(n -> String.format(Locale.US, "%06d", n))
                        .collect(Collectors.toSet());

                if (!codeSet.contains(form.getCode())) {
                    return templateEngine.render("my/signIn/2fa",
                            "signin", form);
                }
            }
        }

        recordSignIn(user, request, form);

        if (user != null) {
            return signIn(user, request, form.getUrl());
        } else {
            form.setErrors(Multimap.of("account", "error.failToSignin"));
            return signInForm(request, form);
        }
    }

    @Transactional
    public HttpResponse signInByClientDN(HttpRequest request, SignInForm form) {
        form.setAccount(getAccountFromClientDN(request));
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user= userDao.selectByAccount(form.getAccount());
        AuditDao auditDao = daoProvider.getDao(AuditDao.class);
        auditDao.insertUserAction(user != null?USER_SIGNIN:USER_FAILED_SIGNIN, form.getAccount(), request.getRemoteAddr());

        if (user != null) {
            return signIn(user, request, form.getUrl());
        } else {
            return templateEngine.render("my/signIn/clientdn",
                    "signin", form);
        }
    }

    public HttpResponse signInByOAuth(HttpRequest request, Parameters params) {
        String oidcSessionId = some(request.getCookies().get("OIDC_SESSION_ID"), Cookie::getValue).orElse(null);
        OidcSession oidcSession = (OidcSession) storeProvider.getStore(OIDC_SESSION).read(oidcSessionId);
        if (!Objects.equals(oidcSession.getState(), params.get("state"))) {
            return signInForm(request, (SignInForm) builder(new SignInForm())
                    .set(SignInForm::setErrors, Multimap.of("account", "error.failToSignin"))
                    .build());
        }


        OidcProviderDao oauth2ProviderDao = daoProvider.getDao(OidcProviderDao.class);
        UserDao userDao = daoProvider.getDao(UserDao.class);
        OidcProvider oauth2Provider = oauth2ProviderDao.selectById(params.getLong("id"));
        OAuth2ProviderDto oauth2ProviderDto = beansConverter.createFrom(oauth2Provider, OAuth2ProviderDto.class);
        oauth2ProviderDto.setRedirectUriBase(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort());

        OkHttpClient okhttp = new OkHttpClient();
        try {
            InputStream in = okhttp.newCall(new Request.Builder()
                    .url(oauth2Provider.getTokenEndpoint())
                    .header("Authorization", "Basic " +
                            Base64.getUrlEncoder().encodeToString((oauth2Provider.getApiKey() + ":" + oauth2Provider.getApiSecret()).getBytes()))
                    .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                            "grant_type=authorization_code&code=" + params.get("code") +
                                    "&redirect_uri=" + oauth2ProviderDto.getRedirectUri()))
                    .build())
                    .execute()
                    .body()
                    .byteStream();
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
            };
            ObjectMapper jsonMapper = new ObjectMapper();
            HashMap<String, Object> map = jsonMapper.readValue(in, typeRef);
            String encodedIdToken = (String) map.get("id_token");
            String[] tokens = encodedIdToken.split("\\.", 3);
            IdTokenPayload payload =idToken.decodePayload(tokens[1]);

            if (payload.getSub() != null) {
                User user = userDao.selectByOAuth2(oauth2Provider.getId(), payload.getSub());
                if (user != null) {
                    return signIn(user, request, params.get("url"));
                } else {
                    Invitation invitation = builder(new Invitation())
                            .set(Invitation::setCode, RandomUtils.generateRandomString(8, config.getSecureRandom()))
                            .build();
                    InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
                    invitationDao.insert(invitation);
                    invitationDao.insert(builder(new OidcInvitation())
                            .set(OidcInvitation::setInvitationId, invitation.getId())
                            .set(OidcInvitation::setOidcProviderId, oauth2Provider.getId())
                            .set(OidcInvitation::setOidcSub, payload.getSub())
                            .build());
                    return UrlRewriter.redirect(SignUpController.class, "newForm?code=" + invitation.getCode(), SEE_OTHER);
                }
            }
            return signInForm(request, new SignInForm());
        } catch (IOException e) {
            throw new FalteringEnvironmentException(e);
        }
    }

    @Transactional
    public HttpResponse signOut(HttpRequest request) {
        some(request.getCookies().get(config.getTokenName()),
                Cookie::getValue).ifPresent(
                token -> {
                    UserSessionDao userSessionDao = daoProvider.getDao(UserSessionDao.class);
                    UserSession thisSession = userSessionDao.selectByToken(token);
                    userSessionDao.delete(thisSession);
                    storeProvider.getStore(BOUNCR_TOKEN).delete(token);
                }
        );
        Cookie expire = builder(Cookie.create(config.getTokenName(), ""))
                .set(Cookie::setPath, "/")
                .set(Cookie::setMaxAge, -1)
                .build();
        return (HttpResponse<String>) builder(UrlRewriter.redirect(SignInController.class, "signInForm", SEE_OTHER))
                .set(HttpResponse::setCookies, Multimap.of(config.getTokenName(), expire))
                .build();
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

    private String getAccountFromClientDN(HttpRequest request) {
        return some(request.getHeaders().get("X-Client-DN"),
                clientDN -> new X500Name(clientDN).getRDNs(BCStyle.CN)[0],
                cn -> IETFUtils.valueToString(cn.getFirst().getValue())).orElse(null);
    }
}
