package net.unit8.bouncr.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import enkan.collection.Headers;
import enkan.collection.Multimap;
import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.exception.FalteringEnvironmentException;
import enkan.util.CodecUtils;
import enkan.util.HttpResponseUtils;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import lombok.Data;
import net.jodah.failsafe.Failsafe;
import net.unit8.bouncr.authz.UserPermissionPrincipal;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.LdapClient;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtClaim;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.controller.data.OidcSession;
import net.unit8.bouncr.web.dao.*;
import net.unit8.bouncr.web.entity.*;
import net.unit8.bouncr.web.form.ForceChangePasswordForm;
import net.unit8.bouncr.web.form.SignInForm;
import net.unit8.bouncr.web.service.PasswordCredentialService;
import net.unit8.bouncr.web.service.SignInService;
import okhttp3.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.builder;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;
import static enkan.util.ThreadingUtils.some;
import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;
import static net.unit8.bouncr.component.StoreProvider.StoreType.OIDC_SESSION;
import static net.unit8.bouncr.web.entity.ActionType.USER_FAILED_SIGNIN;
import static net.unit8.bouncr.web.entity.ActionType.USER_SIGNIN;
import static net.unit8.bouncr.web.entity.ResponseType.ID_TOKEN;
import static net.unit8.bouncr.web.service.SignInService.PasswordCredentialStatus.EXPIRED;
import static net.unit8.bouncr.web.service.SignInService.PasswordCredentialStatus.INITIAL;

public class SignInController {
    private static final TypeReference<HashMap<String, Object>> GENERAL_JSON_REF = new TypeReference<HashMap<String, Object>>() {
    };

    private static final OkHttpClient OKHTTP = new OkHttpClient().newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

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
    private JsonWebToken jsonWebToken;

    @Inject
    private BouncrConfiguration config;

    private SignInService signInService;
    private PasswordCredentialService passwordCredentialService;

    @PostConstruct
    private void initialize() {
        signInService = new SignInService(daoProvider, storeProvider, config);
        passwordCredentialService = new PasswordCredentialService(daoProvider, config);
    }

    public HttpResponse signInForm(HttpRequest request, SignInForm form, UserPermissionPrincipal principal) {
        if (principal != null) {
            return Optional.ofNullable(form.getUrl())
                    .filter(url -> !url.isEmpty())
                    .map(url -> HttpResponseUtils.redirect(url, SEE_OTHER))
                    .orElse(UrlRewriter.redirect(MyController.class, "home", SEE_OTHER));
        }
        String account = getAccountFromClientDN(request);
        if (account != null) {
            form.setAccount(account);
            return templateEngine.render("my/signIn/clientdn",
                    "signin", form);
        } else {
            OidcProviderDao oidcProviderDao = daoProvider.getDao(OidcProviderDao.class);
            String oidcSessionId = UUID.randomUUID().toString();
            OidcSession oidcSession = OidcSession.create(config.getSecureRandom());

            List<OidcProviderDto> oidcProviders = oidcProviderDao
                    .selectAll()
                    .stream()
                    .map(p -> {
                        OidcProviderDto dto = beansConverter.createFrom(p, OidcProviderDto.class);
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
                    "passwordEnabled", config.isPasswordEnabled(),
                    "signUpEnabled", config.isSignUpEnabled(),
                    "oidcProviders", oidcProviders,
                    "signin", form))
                    .set(HttpResponse::setCookies, Multimap.of("OIDC_SESSION_ID", cookie))
                    .build();
        }
    }

    private HttpResponse signInForm(HttpRequest request, SignInForm form) {
        return signInForm(request, form ,null);
    }

    private HttpResponse forceToChangePasswordForm(ForceChangePasswordForm form) {
        return templateEngine.render("my/signIn/force_to_change_password",
                "changePassword", form);
    }

    public HttpResponse forceToChangePassword(ForceChangePasswordForm form, HttpRequest request) {
        passwordCredentialService.validateBasedOnPasswordPolicy(form, "newPassword");

        if (form.hasErrors()) {
            return templateEngine.render("my/signIn/force_to_change_password",
                    "changePassword", form);
        } else {
            UserDao userDao = daoProvider.getDao(UserDao.class);
            User user = userDao.selectByPassword(form.getAccount(), form.getOldPassword());
            if (user == null) {
                return templateEngine.render("my/account",
                        "passwordEnabled", config.isPasswordEnabled(),
                        "message", "error.oldPasswordMismatch",
                        "user", form);
            }

            passwordCredentialService.changePassword(user, form.getNewPassword());
            AuditDao auditDao = daoProvider.getDao(AuditDao.class);
            auditDao.insertUserAction(ActionType.CHANGE_PASSWORD, user.getAccount(), request.getRemoteAddr());

            String token = signInService.signIn(user, request);
            return signInService.responseSignedIn(token, request, form.getUrl());
        }
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
            SignInService.PasswordCredentialStatus status = signInService.validatePasswordCredentialAttributes(user);
            if (status == EXPIRED || status == INITIAL) {
                ForceChangePasswordForm changePasswordForm = new ForceChangePasswordForm();
                changePasswordForm.setAccount(form.getAccount());
                changePasswordForm.setOldPassword(form.getPassword());
                return forceToChangePasswordForm(changePasswordForm);
            }
        }

        if (user != null && !signInService.validateOtpKey(user, form.getCode())) {
            return templateEngine.render("my/signIn/2fa",
                    "signin", form);
        }

        signInService.recordSignIn(user, request, form);

        if (user != null) {
            String token = signInService.signIn(user, request);
            return signInService.responseSignedIn(token, request, form.getUrl());
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
            String token = signInService.signIn(user, request);
            return signInService.responseSignedIn(token, request, form.getUrl());
        } else {
            return templateEngine.render("my/signIn/clientdn",
                    "signin", form);
        }
    }

    private HttpResponse connectOpenIdToBoucrUser(String idToken, OidcProvider oidcProvider, HttpRequest request) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        String[] tokens = idToken.split("\\.", 3);
        JwtClaim claim = jsonWebToken.decodePayload(tokens[1], new TypeReference<JwtClaim>() {});
        // TODO Verify Nonce

        if (claim.getSub() != null) {
            User user = userDao.selectByOidc(oidcProvider.getId(), claim.getSub());
            if (user != null) {
                String token = signInService.signIn(user, request);
                return signInService.responseSignedIn(token, request, request.getParams().get("url"));
            } else {
                Invitation invitation = builder(new Invitation())
                        .set(Invitation::setCode, RandomUtils.generateRandomString(8, config.getSecureRandom()))
                        .build();
                InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
                invitationDao.insert(invitation);
                invitationDao.insert(builder(new OidcInvitation())
                        .set(OidcInvitation::setInvitationId, invitation.getId())
                        .set(OidcInvitation::setOidcProviderId, oidcProvider.getId())
                        .set(OidcInvitation::setOidcPayload, tokens[1])
                        .build());
                if (Objects.equals(request.getHeaders().get("X-Requested-With"), "XMLHttpRequest")) {
                    return builder(templateEngine.render("my/signIn/oidc_implicit_json",
                            "code", invitation.getCode()))
                            .set(HttpResponse::setHeaders, Headers.of("Content-Type", "application/json"))
                            .build();
                } else {
                    return UrlRewriter.redirect(SignUpController.class, "newForm?code=" + invitation.getCode(), SEE_OTHER);
                }
            }
        }
        return signInForm(request, new SignInForm());
    }

    public HttpResponse signInByOidcImplicit(HttpRequest request, Parameters params) {
        String idToken = params.get("id_token");
        OidcProviderDao oidcProviderDao = daoProvider.getDao(OidcProviderDao.class);
        OidcProvider oidcProvider = oidcProviderDao.selectById(params.getLong("id"));
        return connectOpenIdToBoucrUser(idToken, oidcProvider, request);
    }

    public HttpResponse signInByOidc(HttpRequest request, Parameters params) {
        OidcProviderDao oidcProviderDao = daoProvider.getDao(OidcProviderDao.class);
        OidcProvider oidcProvider = oidcProviderDao.selectById(params.getLong("id"));
        if (oidcProvider.getResponseType() == ID_TOKEN || oidcProvider.getResponseType() == ResponseType.TOKEN) {
            return templateEngine.render("my/signIn/oidc_implicit",
                    "oidcProvider", oidcProvider);
        }
        String oidcSessionId = some(request.getCookies().get("OIDC_SESSION_ID"), Cookie::getValue).orElse(null);
        OidcSession oidcSession = (OidcSession) storeProvider.getStore(OIDC_SESSION).read(oidcSessionId);
        if (!Objects.equals(oidcSession.getState(), params.get("state"))) {
            return signInForm(request, (SignInForm) builder(new SignInForm())
                    .set(SignInForm::setErrors, Multimap.of("account", "error.failToSignin"))
                    .build());
        }

        OidcProviderDto oidcProviderDto = beansConverter.createFrom(oidcProvider, OidcProviderDto.class);
        oidcProviderDto.setRedirectUriBase(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort());

        HashMap<String, Object> res = Failsafe.with(config.getHttpClientRetryPolicy()).get(() -> {
            Response response =  OKHTTP.newCall(new Request.Builder()
                    .url(oidcProvider.getTokenEndpoint())
                    .header("Authorization", "Basic " +
                            Base64.getUrlEncoder().encodeToString((oidcProvider.getApiKey() + ":" + oidcProvider.getApiSecret()).getBytes()))
                    .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                            "grant_type=authorization_code&code=" + params.get("code") +
                                    "&redirect_uri=" + oidcProviderDto.getRedirectUri()))
                    .build())
                    .execute();
            if (response.code() == 503) throw new FalteringEnvironmentException();
            try(InputStream in  = response.body().byteStream()) {
                ObjectMapper jsonMapper = new ObjectMapper();
                HashMap<String, Object> map = jsonMapper.readValue(in, GENERAL_JSON_REF);
                return map;
            }
        });
        String encodedIdToken = (String) res.get("id_token");
        return connectOpenIdToBoucrUser(encodedIdToken, oidcProvider, request);
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
        Multimap<String, Cookie> cookies = Multimap.of(config.getTokenName(), expire);
        return builder(UrlRewriter.redirect(SignInController.class, "signInForm", SEE_OTHER))
                .set(HttpResponse::setCookies, cookies)
                .build();
    }

    private String getAccountFromClientDN(HttpRequest request) {
        return some(request.getHeaders().get("X-Client-DN"),
                clientDN -> new X500Name(clientDN).getRDNs(BCStyle.CN)[0],
                cn -> IETFUtils.valueToString(cn.getFirst().getValue())).orElse(null);
    }

    @Data
    public static class OidcProviderDto implements Serializable {
        private Long id;
        private String name;
        private String apiKey;
        private String scope;
        private String state = RandomUtils.generateRandomString(8);
        private String responseType;
        private String tokenEndpoint;
        private String authorizationEndpoint;
        private String nonce = RandomUtils.generateRandomString(32);
        private String redirectUriBase;

        public String getRedirectUri() {
            return redirectUriBase
                    + "/my/signIn/oidc/" + id;
        }

        public String getAuthorizationUrl() {
            return authorizationEndpoint + "?response_type=" + CodecUtils.urlEncode(responseType)
                    + "&client_id=" + apiKey
                    + "&redirect_uri=" + CodecUtils.urlEncode(getRedirectUri())
                    + "&state=" + state
                    + "&scope=" + scope
                    + "&nonce=" + nonce;
        }
    }
}
