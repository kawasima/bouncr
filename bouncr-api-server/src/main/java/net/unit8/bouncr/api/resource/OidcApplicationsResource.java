package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import enkan.security.bouncr.UserPermissionPrincipal;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.decoder.BouncrJsonDecoders;
import net.unit8.bouncr.api.util.PaginationParams;
import net.unit8.bouncr.api.boundary.OidcApplicationCreatedResponse;
import net.unit8.bouncr.api.boundary.OidcApplicationResponse;
import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.api.util.LogoutUriPolicy;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.data.OidcClientMetadata;
import net.unit8.bouncr.data.WordName;
import net.unit8.bouncr.util.KeyEncryptor;
import net.unit8.bouncr.util.KeyUtils;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.api.util.ContextKeys;
import net.unit8.raoh.Err;
import net.unit8.raoh.Ok;
import net.unit8.raoh.decode.combinator.Tuple4;
import org.jooq.DSLContext;
import tools.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static kotowari.restful.DecisionPoint.*;
import static net.unit8.bouncr.api.decoder.BouncrJsonDecoders.toProblem;

@AllowedMethods({"GET", "POST"})
public class OidcApplicationsResource {
    static final ContextKey<Tuple4<WordName, OidcClientMetadata, String, List<String>>> CREATE_REQ =
            ContextKeys.of(Tuple4.class);
    static final ContextKey<OidcApplicationCreatedResponse> RESPONSE = ContextKey.of(OidcApplicationCreatedResponse.class);

    @Inject
    private BouncrConfiguration config;

    @Decision(value = MALFORMED, method = "POST")
    public Problem validateCreate(JsonNode body, RestContext context) {
        if (body == null) {
            return Problem.valueOf(400, "request is empty");
        }
        return switch (BouncrJsonDecoders.OIDC_APPLICATION_CREATE.decode(body)) {
            case Ok(Tuple4(var name, var clientMeta, var desc, var perms)) -> {
                try {
                    var meta = (OidcClientMetadata) clientMeta;
                    LogoutUriPolicy.normalizeBackchannelLogoutUri(
                            meta.backchannelLogoutUri() != null ? meta.backchannelLogoutUri().toString() : null);
                    LogoutUriPolicy.normalizeLogoutUri(
                            meta.frontchannelLogoutUri() != null ? meta.frontchannelLogoutUri().toString() : null);
                    @SuppressWarnings("unchecked")
                    var typedPerms = (List<String>) perms;
                    context.put(CREATE_REQ, new Tuple4<>((WordName) name, meta, (String) desc, typedPerms));
                    yield null;
                } catch (IllegalArgumentException e) {
                    yield Problem.valueOf(400, e.getMessage());
                }
            }
            case Err(var issues) -> toProblem(issues);
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

    @Decision(value = PROCESSABLE, method = "POST")
    public boolean isProcessable(Tuple4<WordName, OidcClientMetadata, String, List<String>> createRequest,
                                 UserPermissionPrincipal principal, RestContext context) {
        List<String> requestedPermissions = createRequest._4();
        if (requestedPermissions != null && !requestedPermissions.isEmpty()) {
            var excess = requestedPermissions.stream()
                    .filter(p -> !principal.permissions().contains(p))
                    .toList();
            if (!excess.isEmpty()) {
                context.setMessage(Problem.valueOf(403,
                        "Cannot grant permissions you do not have: " + excess));
                return false;
            }
        }
        return true;
    }

    @Decision(value = CONFLICT, method = "POST")
    public boolean isConflict(Tuple4<WordName, OidcClientMetadata, String, List<String>> createRequest, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        return !repo.isNameUnique(createRequest._1());
    }

    @Decision(HANDLE_OK)
    public List<OidcApplicationResponse> list(Parameters params, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        String q = params.get("q");
        int offset = PaginationParams.parseOffset(params.get("offset"));
        int limit = PaginationParams.parseLimit(params.get("limit"), 10);
        return repo.search(q, offset, limit).stream()
                .map(OidcApplicationResponse::of)
                .toList();
    }

    @Decision(POST)
    public boolean create(Tuple4<WordName, OidcClientMetadata, String, List<String>> createRequest, RestContext context, DSLContext dsl) {
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

        var meta = createRequest._2();
        OidcApplication app = repo.insert(
                createRequest._1().value(),
                clientId,
                hashedSecret,
                privateKey,
                publicKey,
                meta.homeUri() != null ? meta.homeUri().toString() : null,
                meta.callbackUri() != null ? meta.callbackUri().toString() : null,
                createRequest._3(),
                LogoutUriPolicy.normalizeBackchannelLogoutUri(
                        meta.backchannelLogoutUri() != null ? meta.backchannelLogoutUri().toString() : null),
                LogoutUriPolicy.normalizeLogoutUri(
                        meta.frontchannelLogoutUri() != null ? meta.frontchannelLogoutUri().toString() : null)
        );

        if (createRequest._4() != null && !createRequest._4().isEmpty()) {
            repo.setPermissions(app.id(), createRequest._4());
        }

        repo.setGrantTypes(app.id(), meta.grantTypes());

        // Return plaintext client_secret once (never stored or retrievable again)
        OidcApplication saved = repo.findByName(createRequest._1().value()).orElse(app);
        context.put(RESPONSE, OidcApplicationCreatedResponse.of(saved, plaintextSecret));
        return true;
    }

    @Decision(HANDLE_CREATED)
    public OidcApplicationCreatedResponse handleCreated(OidcApplicationCreatedResponse response) {
        return response;
    }
}
