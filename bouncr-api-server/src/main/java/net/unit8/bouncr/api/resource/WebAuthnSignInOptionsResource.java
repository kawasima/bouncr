package net.unit8.bouncr.api.resource;

import enkan.collection.Headers;
import kotowari.restful.Decision;
import kotowari.restful.data.ApiResponse;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.util.BouncrCookies;
import net.unit8.bouncr.api.boundary.WebAuthnAuthenticationOptions;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.boundary.WebAuthnSignInOptions;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;
import static net.unit8.bouncr.component.StoreProvider.StoreType.WEBAUTHN_CHALLENGE;

@AllowedMethods("POST")
public class WebAuthnSignInOptionsResource {
    private static final String COOKIE_NAME = "WEBAUTHN_SESSION_ID";
    static final ContextKey<WebAuthnSignInOptions> REQ = ContextKey.of(WebAuthnSignInOptions.class);

    record PostResult(WebAuthnAuthenticationOptions options, String sessionId) {}
    static final ContextKey<PostResult> RESULT = ContextKey.of(PostResult.class);

    @Inject
    private BouncrConfiguration config;

    @Inject
    private StoreProvider storeProvider;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validate(JsonNode body, RestContext context) {
        if (body == null) {
            context.put(REQ, new WebAuthnSignInOptions(null));
            return null;
        }
        return switch (BouncrJsonDecoders.WEBAUTHN_SIGN_IN_OPTIONS.decode(body)) {
            case Ok<WebAuthnSignInOptions> ok -> { context.put(REQ, ok.value()); yield null; }
            case Err<WebAuthnSignInOptions>(var issues) -> toProblem(issues);
        };
    }

    @Decision(POST)
    public boolean doPost(WebAuthnSignInOptions request,
                          RestContext context,
                          DSLContext dsl) {
        WebAuthnService webAuthnService = new WebAuthnService(config);
        byte[] challenge = webAuthnService.generateChallenge();
        Base64.Encoder b64url = Base64.getUrlEncoder().withoutPadding();

        List<WebAuthnAuthenticationOptions.AllowCredential> allowCredentials = List.of();
        Long userId = null;

        if (request.account() != null) {
            UserRepository userRepo = new UserRepository(dsl);
            Optional<User> userOpt = userRepo.findByAccount(request.account());
            if (userOpt.isPresent()) {
                userId = userOpt.get().id();
                WebAuthnCredentialRepository credRepo = new WebAuthnCredentialRepository(dsl);
                allowCredentials = credRepo.findByUserId(userId).stream()
                        .map(c -> new WebAuthnAuthenticationOptions.AllowCredential(
                                "public-key",
                                b64url.encodeToString(c.credentialId()),
                                c.transports() != null && !c.transports().isEmpty()
                                        ? List.of(c.transports().split(","))
                                        : List.of()))
                        .toList();
            } else {
                userId = -1L;
            }
        }

        String sessionId = UUID.randomUUID().toString();
        storeProvider.getStore(WEBAUTHN_CHALLENGE).write(sessionId,
                new WebAuthnChallenge(challenge, userId, config.getWebAuthnRpId(), WebAuthnChallenge.TYPE_AUTHENTICATION));

        WebAuthnAuthenticationOptions options = new WebAuthnAuthenticationOptions(
                b64url.encodeToString(challenge),
                config.getWebAuthnRpId(),
                allowCredentials,
                "preferred");

        context.put(RESULT, new PostResult(options, sessionId));
        return true;
    }

    @Decision(HANDLE_CREATED)
    public ApiResponse handleCreated(PostResult result) {
        String cookieStr = new BouncrCookies(config)
                .session(COOKIE_NAME, result.sessionId(), (int) config.getWebAuthnChallengeExpires())
                .toHttpString();

        return builder(new ApiResponse())
                .set(ApiResponse::setStatus, 201)
                .set(ApiResponse::setHeaders, Headers.of("Set-Cookie", cookieStr))
                .set(ApiResponse::setBody, result.options())
                .build();
    }
}
