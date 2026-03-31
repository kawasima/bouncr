package net.unit8.bouncr.api.decoder;

import kotowari.restful.data.Problem;
import net.unit8.bouncr.api.repository.AssignmentRepository;
import net.unit8.bouncr.api.repository.UserProfileFieldRepository;
import net.unit8.bouncr.data.*;
import net.unit8.raoh.decode.Decoder;
import net.unit8.raoh.Issue;
import net.unit8.raoh.Issues;
import net.unit8.raoh.Path;
import net.unit8.raoh.Presence;
import net.unit8.raoh.Result;
import net.unit8.raoh.decode.combinator.*;
import net.unit8.raoh.json.JsonDecoder;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static net.unit8.raoh.decode.Decoders.withDefault;
import static net.unit8.raoh.json.JsonDecoders.*;

/**
 * Centralized raoh-json decoders for all API request types.
 */
public final class BouncrJsonDecoders {
    private BouncrJsonDecoders() {}

    public static Problem toProblem(Issues issues) {
        return Problem.fromViolationList(issues.asList().stream()
                .map(i -> new Problem.Violation(i.path().toString(), i.code(), i.message()))
                .toList());
    }

    // --- Reusable field decoders ---

    private static final Pattern WORD_PATTERN = Pattern.compile("^\\w+$");
    private static final Pattern PERMISSION_PATTERN = Pattern.compile("^[\\w:]+$");
    private static final Decoder<JsonNode, WordName> WORD_NAME = string().nonBlank().maxLength(100).pattern(WORD_PATTERN).map(WordName::new);
    private static final Decoder<JsonNode, PermissionName> PERMISSION_NAME = string().nonBlank().maxLength(100).pattern(PERMISSION_PATTERN).map(PermissionName::new);
    private static final Decoder<JsonNode, String> PASSWORD = string().nonBlank().maxLength(300);

    private static Decoder<JsonNode, String> httpUrl(int maxLength) {
        return string().maxLength(maxLength).flatMap(url -> {
            if (url.isBlank()) return Result.ok((String) null);
            try {
                URI uri = URI.create(url.trim());
                if (!uri.isAbsolute() || (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))) {
                    return Result.fail("invalid", "must be an absolute http or https URL");
                }
                return Result.ok(url.trim());
            } catch (IllegalArgumentException e) {
                return Result.fail("invalid", "not a valid URI");
            }
        });
    }

    private static Decoder<JsonNode, URI> httpUri(int maxLength) {
        return string().maxLength(maxLength).flatMap(url -> {
            if (url.isBlank()) return Result.ok((URI) null);
            try {
                URI uri = URI.create(url.trim());
                if (!uri.isAbsolute() || (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))) {
                    return Result.fail("invalid", "must be an absolute http or https URL");
                }
                return Result.ok(uri);
            } catch (IllegalArgumentException e) {
                return Result.fail("invalid", "not a valid URI");
            }
        });
    }

    // ===== Application =====

    public static final JsonDecoder<ApplicationSpec> APPLICATION_SPEC = combine(
            field("name", WORD_NAME),
            withDefault(field("description", string().nonBlank()), (String) null),
            field("virtual_path", string().nonBlank().maxLength(100)),
            field("pass_to", string().nonBlank().maxLength(100)),
            withDefault(field("top_page", string().maxLength(100)), (String) null)
    ).map(ApplicationSpec::new)::decode;

    // ===== Group =====

    public static final JsonDecoder<GroupSpec> GROUP_SPEC = combine(
            field("name", WORD_NAME),
            withDefault(field("description", string().nonBlank()), (String) null)
    ).map(GroupSpec::new)::decode;

    // ===== Role =====

    public static final JsonDecoder<RoleSpec> ROLE_SPEC = combine(
            field("name", WORD_NAME),
            withDefault(field("description", string().nonBlank()), (String) null)
    ).map(RoleSpec::new)::decode;

    // ===== Permission =====

    public static final JsonDecoder<PermissionSpec> PERMISSION_SPEC = combine(
            field("name", PERMISSION_NAME),
            withDefault(field("description", string().nonBlank()), (String) null)
    ).map(PermissionSpec::new)::decode;

    // ===== Realm =====

    public static final JsonDecoder<RealmSpec> REALM_SPEC = combine(
            field("name", WORD_NAME),
            withDefault(field("url", string().nonBlank()), (String) null),
            withDefault(field("description", string().nonBlank()), (String) null)
    ).map(RealmSpec::new)::decode;

    // ===== Invitation =====

    public static final JsonDecoder<Tuple2<Email, List<EntityRef>>> INVITATION_CREATE = combine(
            field("email", string().email().map(Email::new)),
            withDefault(field("groups", list(field("id", long_()).map(EntityRef::ofId))), List.of())
    ).map(Tuple2::new)::decode;

    // ===== Password Sign In =====

    public static final JsonDecoder<Tuple3<WordName, String, String>> PASSWORD_SIGN_IN = combine(
            field("account", WORD_NAME),
            field("password", PASSWORD),
            withDefault(field("one_time_password", string().maxLength(100)), (String) null)
    ).map(Tuple3::new)::decode;

    // ===== Password Credential =====

    public static final JsonDecoder<Tuple3<WordName, String, Boolean>> PASSWORD_CREDENTIAL_CREATE = combine(
            field("account", WORD_NAME),
            field("password", PASSWORD),
            withDefault(field("initial", bool()), true)
    ).map(Tuple3::new)::decode;

    public static final JsonDecoder<Tuple3<WordName, String, String>> PASSWORD_CREDENTIAL_UPDATE = combine(
            withDefault(field("account", WORD_NAME), (WordName) null),
            field("old_password", PASSWORD),
            field("new_password", PASSWORD)
    ).map(Tuple3::new)::decode;

    public static final JsonDecoder<Tuple2<WordName, String>> PASSWORD_CREDENTIAL_DELETE = combine(
            field("account", WORD_NAME),
            field("password", PASSWORD)
    ).map(Tuple2::new)::decode;

    // ===== Password Reset Challenge =====

    public static final JsonDecoder<WordName> PASSWORD_RESET_CHALLENGE_CREATE =
            field("account", WORD_NAME)::decode;

    // ===== Password Reset =====

    public static final JsonDecoder<String> PASSWORD_RESET =
            field("code", string())::decode;

    // ===== Token Refresh =====

    public static final JsonDecoder<String> TOKEN_REFRESH =
            field("session_id", string().nonBlank())::decode;

    // ===== Role Permissions =====
    public static final JsonDecoder<List<String>> ROLE_PERMISSIONS = list(string())::decode;

    // ===== Group Users =====
    public static final JsonDecoder<List<String>> GROUP_USERS = list(string())::decode;

    // ===== Assignment =====

    private static final JsonDecoder<EntityRef> ID_OBJECT = combine(
            withDefault(field("id", long_()), (Long) null),
            withDefault(field("name", string()), (String) null)
    ).map(EntityRef::new)::decode;

    public static final JsonDecoder<List<AssignmentRef>> ASSIGNMENTS = list(
            combine(
                    field("group", ID_OBJECT),
                    field("role", ID_OBJECT),
                    field("realm", ID_OBJECT)
            ).map(AssignmentRef::new)
    )::decode;

    private static JsonDecoder<Long> resolvedId(AssignmentRepository repo, String tableName, String idColumn) {
        return (in, path) -> ID_OBJECT.decode(in, path).flatMap(idObj -> {
            if (idObj.id() != null) {
                return Result.ok(idObj.id());
            } else if (idObj.name() != null) {
                Long resolved = repo.resolveIdByName(tableName, idColumn, idObj.name());
                return resolved != null
                        ? Result.ok(resolved)
                        : Result.fail(path, "not_found", "not found");
            }
            return Result.fail(path, "required", "id or name is required");
        });
    }

    public static JsonDecoder<List<AssignmentId>> assignments(AssignmentRepository repo) {
        return list(
                combine(
                        field("group", resolvedId(repo, "groups", "group_id")),
                        field("role", resolvedId(repo, "roles", "role_id")),
                        field("realm", resolvedId(repo, "realms", "realm_id"))
                ).map(AssignmentId::new)
        )::decode;
    }

    // ===== OIDC Provider =====

    public static final JsonDecoder<Tuple3<WordName, OidcProviderMetadata, OidcProviderClientConfig>> OIDC_PROVIDER_CREATE = combine(
            field("name", WORD_NAME),
            field("authorization_endpoint", string().nonBlank().maxLength(255)),
            field("token_endpoint", string().nonBlank().maxLength(255)),
            withDefault(field("jwks_uri", httpUri(512)), (URI) null),
            withDefault(field("issuer", string().maxLength(512)), (String) null),
            field("client_id", string().nonBlank().maxLength(255)),
            field("client_secret", string().nonBlank().maxLength(255)),
            field("scope", string().nonBlank().maxLength(255)),
            field("response_type", string().nonBlank().maxLength(16)),
            field("token_endpoint_auth_method", string().nonBlank()),
            field("redirect_uri", string().nonBlank().maxLength(255).uri()),
            withDefault(field("pkce_enabled", bool()), false)
    ).<Tuple3<WordName, OidcProviderMetadata, OidcProviderClientConfig>>flatMap(
            (name, ae, te, jwks, iss, cid, cs, scope, rt, team, ru, pkce) -> {
                ResponseType responseType;
                try {
                    responseType = ResponseType.of(rt);
                } catch (IllegalArgumentException e) {
                    return Result.fail(Path.ROOT.append("response_type"), "invalid",
                            "unknown response_type: " + rt);
                }
                TokenEndpointAuthMethod authMethod;
                try {
                    authMethod = TokenEndpointAuthMethod.of(team);
                } catch (IllegalArgumentException e) {
                    return Result.fail(Path.ROOT.append("token_endpoint_auth_method"), "invalid",
                            "unknown token_endpoint_auth_method: " + team);
                }
                return Result.ok(new Tuple3<>(name,
                        new OidcProviderMetadata(ae, te, jwks, iss),
                        new OidcProviderClientConfig(new ClientCredentials(cid, cs), scope,
                                responseType, authMethod, ru, pkce)));
            })::decode;

    // Update uses the same decoder as create — all fields are required for full replacement.
    // If partial update is needed in the future, define a separate decoder here.
    public static final JsonDecoder<Tuple3<WordName, OidcProviderMetadata, OidcProviderClientConfig>> OIDC_PROVIDER_UPDATE = OIDC_PROVIDER_CREATE;

    // ===== OIDC Application =====

    public static final JsonDecoder<Tuple4<WordName, OidcClientMetadata, String, List<String>>> OIDC_APPLICATION_CREATE = combine(
            field("name", WORD_NAME),
            field("grant_types", list(string())),
            withDefault(field("home_uri", httpUri(2048)), (URI) null),
            withDefault(field("callback_uri", httpUri(2048)), (URI) null),
            withDefault(field("description", string().maxLength(255)), (String) null),
            withDefault(field("backchannel_logout_uri", httpUri(2048)), (URI) null),
            withDefault(field("frontchannel_logout_uri", httpUri(2048)), (URI) null),
            withDefault(field("permissions", list(string())), List.of())
    ).<Tuple4<WordName, OidcClientMetadata, String, List<String>>>flatMap((name, gt, hu, cu, desc, bcu, fcu, perms) ->
            validateOidcAppGrantTypes(gt, cu != null ? cu.toString() : null, hu != null ? hu.toString() : null).map(v ->
                    new Tuple4<>(name,
                            new OidcClientMetadata(hu, cu, bcu, fcu, GrantType.parseAll(gt)),
                            desc, perms)))::decode;

    public static final JsonDecoder<OidcApplicationUpdateSpec> OIDC_APPLICATION_UPDATE = combine(
            field("name", WORD_NAME),
            field("grant_types", list(string())),
            optionalNullableField("home_uri", httpUrl(2048)),
            optionalNullableField("callback_uri", httpUrl(2048)),
            optionalNullableField("description", string().maxLength(255)),
            optionalNullableField("backchannel_logout_uri", httpUrl(2048)),
            optionalNullableField("frontchannel_logout_uri", httpUrl(2048)),
            withDefault(field("permissions", list(string())), List.of())
    ).<OidcApplicationUpdateSpec>flatMap((name, gt, hu, cu, desc, bcu, fcu, perms) ->
            validateOidcAppGrantTypes(gt, presenceToNullable(cu), presenceToNullable(hu)).map(v ->
                    new OidcApplicationUpdateSpec(name, gt, hu, cu, desc, bcu, fcu, perms)))::decode;

    // ===== Sign Up =====

    public static final JsonDecoder<Tuple3<WordName, String, Boolean>> SIGN_UP = combine(
            field("account", WORD_NAME),
            withDefault(field("code", string()), (String) null),
            withDefault(field("enable_password_credential", bool()), true)
    ).map(Tuple3::new)::decode;

    // ===== User Create (admin) =====

    public static final JsonDecoder<WordName> USER_CREATE =
            field("account", WORD_NAME)::decode;

    // ===== User Profile (dynamic) =====

    public static JsonDecoder<UserProfile> userProfile(UserProfileFieldRepository fieldRepo) {
        List<UserProfileField> fields = fieldRepo.findAll();
        return (in, path) -> {
            var issues = Issues.EMPTY;
            var values = new LinkedHashMap<String, String>();

            for (UserProfileField f : fields) {
                var fieldPath = path.append(f.jsonName());
                var node = in.get(f.jsonName());

                if (node == null || node.isNull() || node.isMissingNode()) {
                    if (f.isRequired()) {
                        issues = issues.add(Issue.of(fieldPath, "required", "is required"));
                    }
                    continue;
                }
                String value = node.asString();

                if (f.minLength() != null && value.length() < f.minLength()) {
                    issues = issues.add(Issue.of(fieldPath, "min_length",
                            "must be at least " + f.minLength() + " characters",
                            Map.of("min", f.minLength())));
                }
                if (f.maxLength() != null && value.length() > f.maxLength()) {
                    issues = issues.add(Issue.of(fieldPath, "max_length",
                            "must be at most " + f.maxLength() + " characters",
                            Map.of("max", f.maxLength())));
                }
                if (f.regularExpression() != null
                        && !Pattern.compile(f.regularExpression()).matcher(value).matches()) {
                    issues = issues.add(Issue.of(fieldPath, "pattern",
                            "does not match required pattern"));
                }
                values.put(f.jsonName(), value);
            }

            return issues.isEmpty()
                    ? Result.ok(new UserProfile(Map.copyOf(values)))
                    : Result.err(issues);
        };
    }

    // ===== WebAuthn =====

    public static final JsonDecoder<Tuple2<String, String>> WEBAUTHN_REGISTER = combine(
            field("registration_response_json", string().nonBlank()),
            withDefault(field("credential_name", string().maxLength(100)), (String) null)
    ).map(Tuple2::new)::decode;

    public static final JsonDecoder<String> WEBAUTHN_AUTHENTICATE =
            field("authentication_response_json", string().nonBlank())::decode;

    public static final JsonDecoder<String> WEBAUTHN_SIGN_IN_OPTIONS =
            withDefault(field("account", string().maxLength(100)), (String) null)::decode;

    // ===== Helpers =====

    public static <T> T presenceToNullable(Presence<T> p) {
        return p instanceof Presence.Present<T> present ? present.value() : null;
    }

    private static Result<Void> validateOidcAppGrantTypes(List<String> grantTypes, String callbackUrl, String homeUrl) {
        if (grantTypes.isEmpty()) {
            return Result.fail(Path.ROOT.append("grant_types"), "required", "at least one grant type is required");
        }
        for (String gt : grantTypes) {
            if (net.unit8.bouncr.data.GrantType.fromString(gt).isEmpty()) {
                return Result.fail(Path.ROOT.append("grant_types"), "invalid", "unknown grant_type: " + gt);
            }
        }
        if (grantTypes.contains("authorization_code") && (callbackUrl == null || callbackUrl.isBlank())) {
            return Result.fail(Path.ROOT.append("callback_uri"), "required",
                    "callback_uri is required when authorization_code grant is enabled");
        }
        return Result.ok(null);
    }
}
