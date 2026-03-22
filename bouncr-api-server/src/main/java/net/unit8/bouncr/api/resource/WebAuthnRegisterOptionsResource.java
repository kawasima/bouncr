package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.WebAuthnRegistrationOptions;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.repository.WebAuthnCredentialRepository;
import net.unit8.bouncr.api.service.WebAuthnService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.WebAuthnChallenge;
import net.unit8.bouncr.data.WebAuthnCredential;
import org.jooq.DSLContext;

import jakarta.inject.Inject;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.WEBAUTHN_CHALLENGE;

@AllowedMethods("POST")
public class WebAuthnRegisterOptionsResource {
    @Inject
    private BouncrConfiguration config;

    @Inject
    private StoreProvider storeProvider;

    private static final String COOKIE_NAME = "WEBAUTHN_SESSION_ID";

    record PostResult(WebAuthnRegistrationOptions options, String sessionId) {}
    static final ContextKey<PostResult> RESULT = ContextKey.of(PostResult.class);

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean allowed(UserPermissionPrincipal principal) {
        return principal.hasPermission("my:update");
    }

    @Decision(POST)
    public boolean doPost(UserPermissionPrincipal principal,
                          RestContext context,
                          DSLContext dsl) {
        UserRepository userRepo = new UserRepository(dsl);
        User user = userRepo.findByAccount(principal.getName()).orElseThrow();
        WebAuthnCredentialRepository credRepo = new WebAuthnCredentialRepository(dsl);
        WebAuthnService webAuthnService = new WebAuthnService(config);

        byte[] challenge = webAuthnService.generateChallenge();
        List<WebAuthnCredential> existing = credRepo.findByUserId(user.id());

        String sessionId = UUID.randomUUID().toString();
        storeProvider.getStore(WEBAUTHN_CHALLENGE).write(sessionId,
                new WebAuthnChallenge(challenge, user.id(), config.getWebAuthnRpId(), WebAuthnChallenge.TYPE_REGISTRATION));

        Base64.Encoder b64url = Base64.getUrlEncoder().withoutPadding();

        WebAuthnRegistrationOptions options = new WebAuthnRegistrationOptions(
                b64url.encodeToString(challenge),
                new WebAuthnRegistrationOptions.RelyingParty(config.getWebAuthnRpId(), config.getWebAuthnRpName()),
                new WebAuthnRegistrationOptions.UserEntity(
                        b64url.encodeToString(WebAuthnService.userIdToHandle(user.id())),
                        user.account(),
                        user.account()),
                List.of(
                        new WebAuthnRegistrationOptions.PubKeyCredParam("public-key", -7),   // ES256
                        new WebAuthnRegistrationOptions.PubKeyCredParam("public-key", -257)  // RS256
                ),
                existing.stream()
                        .map(c -> new WebAuthnRegistrationOptions.CredentialDescriptor(
                                "public-key", b64url.encodeToString(c.credentialId())))
                        .toList(),
                Map.of("residentKey", "preferred", "userVerification", "preferred"),
                "none");

        context.put(RESULT, new PostResult(options, sessionId));
        return true;
    }

    @Decision(HANDLE_CREATED)
    public ApiResponse handleCreated(PostResult result) {
        String cookieStr = COOKIE_NAME + "=" + result.sessionId()
                + "; HttpOnly; SameSite=Lax; Max-Age=" + config.getWebAuthnChallengeExpires()
                + "; Path=/" + (config.isSecureCookie() ? "; Secure" : "");

        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 201)
                .set(ApiResponse::setHeaders, Headers.of("Set-Cookie", cookieStr))
                .set(ApiResponse::setBody, result.options())
                .build();
    }
}
