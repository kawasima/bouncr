package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders.OidcApplicationCreate;
import net.unit8.bouncr.api.boundary.OidcApplicationCreatedResponse;
import net.unit8.bouncr.api.boundary.OidcApplicationResponse;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.api.util.LogoutUriPolicy;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.util.KeyEncryptor;
import net.unit8.bouncr.util.KeyUtils;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.security.KeyPair;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST"})
public class OidcApplicationsResoruce {
    static final ContextKey<OidcApplicationCreate> CREATE_REQ = ContextKey.of(OidcApplicationCreate.class);
    static final ContextKey<OidcApplicationCreatedResponse> RESPONSE = ContextKey.of(OidcApplicationCreatedResponse.class);

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.OIDC_APPLICATION_CREATE.decode(body)) {
            case Ok<OidcApplicationCreate> ok -> {
                try {
                    LogoutUriPolicy.normalizeBackchannelLogoutUri(ok.value().backchannelLogoutUri());
                    LogoutUriPolicy.normalizeLogoutUri(ok.value().frontchannelLogoutUri());
                    context.put(CREATE_REQ, ok.value());
                    yield null;
                } catch (IllegalArgumentException e) {
                    yield Problem.valueOf(400, e.getMessage());
                }
            }
            case Err<OidcApplicationCreate>(var issues) -> toProblem(issues);
        };
    }

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method = "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_application:read"))
                .isPresent();
    }

    @Decision(value = ALLOWED, method = "POST")
    public boolean isPostAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("oidc_application:create"))
                .isPresent();
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(OidcApplicationCreate createRequest, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        return !repo.isNameUnique(createRequest.name());
    }

    @Decision(HANDLE_OK)
    public List<OidcApplicationResponse> list(Parameters params, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        String q = params.get("q");
        int offset = Optional.ofNullable(params.<String>get("offset")).map(Integer::parseInt).orElse(0);
        int limit = Optional.ofNullable(params.<String>get("limit")).map(Integer::parseInt).orElse(10);
        return repo.search(q, offset, limit).stream()
                .map(OidcApplicationResponse::of)
                .toList();
    }

    @Decision(POST)
    public boolean create(OidcApplicationCreate createRequest, RestContext context, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);

        String clientId = RandomUtils.generateRandomString(16, config.getSecureRandom());
        String plaintextSecret = RandomUtils.generateRandomString(32, config.getSecureRandom());

        // Hash client_secret with PBKDF2 (salt = clientId)
        byte[] secretHash = PasswordUtils.pbkdf2(plaintextSecret, clientId, config.getPbkdf2Iterations());
        String hashedSecret = Base64.getEncoder().encodeToString(secretHash);

        KeyPair keyPair = KeyUtils.generate(2048, config.getSecureRandom());
        byte[] publicKey = keyPair.getPublic().getEncoded();
        byte[] privateKeyRaw = keyPair.getPrivate().getEncoded();

        // Encrypt private key at rest (no-op if keyEncryptionKey not configured)
        KeyEncryptor encryptor = new KeyEncryptor(config.getKeyEncryptionKey(), config.getSecureRandom());
        byte[] privateKey = encryptor.encrypt(privateKeyRaw);

        OidcApplication app = repo.insert(
                createRequest.name(),
                clientId,
                hashedSecret,
                privateKey,
                publicKey,
                createRequest.homeUrl(),
                createRequest.callbackUrl(),
                createRequest.description(),
                LogoutUriPolicy.normalizeBackchannelLogoutUri(createRequest.backchannelLogoutUri()),
                LogoutUriPolicy.normalizeLogoutUri(createRequest.frontchannelLogoutUri())
        );

        if (createRequest.permissions() != null && !createRequest.permissions().isEmpty()) {
            repo.setPermissions(app.id(), createRequest.permissions());
        }

        // Return plaintext client_secret once (never stored or retrievable again)
        OidcApplication saved = repo.findByName(createRequest.name()).orElse(app);
        context.put(RESPONSE, OidcApplicationCreatedResponse.of(saved, plaintextSecret));
        return true;
    }

    @Decision(HANDLE_CREATED)
    public OidcApplicationCreatedResponse handleCreated(OidcApplicationCreatedResponse response) {
        return response;
    }
}
