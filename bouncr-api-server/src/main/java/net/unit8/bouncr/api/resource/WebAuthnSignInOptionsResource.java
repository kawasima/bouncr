package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.WebAuthnSignInOptions;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.api.repository.WebAuthnCredentialRepository;
import net.unit8.bouncr.api.service.WebAuthnService;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.data.WebAuthnChallenge;

import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;
import static net.unit8.bouncr.component.StoreProvider.StoreType.WEBAUTHN_CHALLENGE;

@AllowedMethods("POST")
public class WebAuthnSignInOptionsResource {
    private static final String COOKIE_NAME = "WEBAUTHN_SESSION_ID";
    static final ContextKey<WebAuthnSignInOptions> REQ = ContextKey.of(WebAuthnSignInOptions.class);

    @Inject
    private BouncrConfiguration config;

    @Inject
    private StoreProvider storeProvider;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validate(JsonNode body, RestContext context) {
        if (body == null) {
            // Allow empty body for discoverable credential flow
            context.put(REQ, new WebAuthnSignInOptions(null));
            return null;
        }
        return switch (BouncrJsonDecoders.WEBAUTHN_SIGN_IN_OPTIONS.decode(body)) {
            case Ok<WebAuthnSignInOptions> ok -> { context.put(REQ, ok.value()); yield null; }
            case Err<WebAuthnSignInOptions>(var issues) -> toProblem(issues);
        };
    }

    @Decision(HANDLE_CREATED)
    public ApiResponse handleCreated(WebAuthnSignInOptions request,
                                     RestContext context,
                                     DSLContext dsl) {
        WebAuthnService webAuthnService = new WebAuthnService(config);
        byte[] challenge = webAuthnService.generateChallenge();
        Base64.Encoder b64url = Base64.getUrlEncoder().withoutPadding();

        List<Map<String, Object>> allowCredentials = List.of();
        Long userId = null;

        if (request.account() != null) {
            UserRepository userRepo = new UserRepository(dsl);
            Optional<User> userOpt = userRepo.findByAccount(request.account());
            if (userOpt.isPresent()) {
                userId = userOpt.get().id();
                WebAuthnCredentialRepository credRepo = new WebAuthnCredentialRepository(dsl);
                allowCredentials = credRepo.findByUserId(userId).stream()
                        .map(c -> {
                            Map<String, Object> desc = new LinkedHashMap<>();
                            desc.put("type", "public-key");
                            desc.put("id", b64url.encodeToString(c.credentialId()));
                            if (c.transports() != null && !c.transports().isEmpty()) {
                                desc.put("transports", List.of(c.transports().split(",")));
                            }
                            return desc;
                        })
                        .toList();
            } else {
                // Bind challenge to a sentinel userId so the cross-check in
                // WebAuthnSignInResource always rejects, without revealing
                // whether the account exists (user enumeration prevention).
                userId = -1L;
            }
        }

        String sessionId = UUID.randomUUID().toString();
        storeProvider.getStore(WEBAUTHN_CHALLENGE).write(sessionId,
                new WebAuthnChallenge(challenge, userId, config.getWebAuthnRpId(), WebAuthnChallenge.TYPE_AUTHENTICATION));

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("challenge", b64url.encodeToString(challenge));
        options.put("rpId", config.getWebAuthnRpId());
        options.put("allowCredentials", allowCredentials);
        options.put("userVerification", "preferred");

        String cookieStr = COOKIE_NAME + "=" + sessionId
                + "; HttpOnly; SameSite=Lax; Max-Age=" + config.getWebAuthnChallengeExpires()
                + "; Path=/" + (config.isSecureCookie() ? "; Secure" : "");

        ApiResponse response = new ApiResponse();
        response.setStatus(201);
        response.setHeaders(Headers.of("Set-Cookie", cookieStr));
        response.setBody(options);
        return response;
    }
}
