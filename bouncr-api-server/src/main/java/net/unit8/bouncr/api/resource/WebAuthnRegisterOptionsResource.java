package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.component.StoreProvider.StoreType.WEBAUTHN_CHALLENGE;

@AllowedMethods("POST")
public class WebAuthnRegisterOptionsResource {
    @Inject
    private BouncrConfiguration config;

    @Inject
    private StoreProvider storeProvider;

    private static final String COOKIE_NAME = "WEBAUTHN_SESSION_ID";

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean allowed(UserPermissionPrincipal principal) {
        return principal.hasPermission("my:update");
    }

    @Decision(HANDLE_CREATED)
    public ApiResponse handleCreated(UserPermissionPrincipal principal,
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

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("challenge", b64url.encodeToString(challenge));
        options.put("rp", Map.of("id", config.getWebAuthnRpId(), "name", config.getWebAuthnRpName()));
        options.put("user", Map.of(
                "id", b64url.encodeToString(WebAuthnService.userIdToHandle(user.id())),
                "name", user.account(),
                "displayName", user.account()));
        options.put("pubKeyCredParams", List.of(
                Map.of("type", "public-key", "alg", -7),   // ES256
                Map.of("type", "public-key", "alg", -257)  // RS256
        ));
        options.put("excludeCredentials", existing.stream()
                .map(c -> Map.of(
                        "type", "public-key",
                        "id", b64url.encodeToString(c.credentialId())))
                .toList());
        options.put("authenticatorSelection", Map.of(
                "residentKey", "preferred",
                "userVerification", "preferred"));
        options.put("attestation", "none");

        String cookieStr = COOKIE_NAME + "=" + sessionId
                + "; HttpOnly; SameSite=Lax; Max-Age=" + config.getWebAuthnChallengeExpires()
                + "; Path=/";

        ApiResponse response = new ApiResponse();
        response.setStatus(201);
        response.setHeaders(Headers.of("Set-Cookie", cookieStr));
        response.setBody(options);
        return response;
    }
}
