package net.unit8.bouncr.api.decoder;

import kotowari.restful.data.Problem;
import net.unit8.bouncr.api.boundary.*;
import net.unit8.bouncr.api.repository.AssignmentRepository;
import net.unit8.bouncr.api.repository.UserProfileFieldRepository;
import net.unit8.bouncr.data.UserProfile;
import net.unit8.bouncr.data.UserProfileField;
import net.unit8.raoh.Decoder;
import net.unit8.raoh.Issue;
import net.unit8.raoh.Issues;
import net.unit8.raoh.Path;
import net.unit8.raoh.Presence;
import net.unit8.raoh.Result;
import net.unit8.raoh.json.JsonDecoder;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static net.unit8.raoh.Decoders.withDefault;
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
    private static final Decoder<JsonNode, String> WORD_NAME = string().nonBlank().maxLength(100).pattern(WORD_PATTERN);
    private static final Decoder<JsonNode, String> PERMISSION_NAME = string().nonBlank().maxLength(100).pattern(PERMISSION_PATTERN);
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

    // ===== Application =====

    public static final JsonDecoder<ApplicationCreate> APPLICATION_CREATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank()),
            field("virtual_path", string().nonBlank().maxLength(100)),
            field("pass_to", string().nonBlank().maxLength(100)),
            withDefault(field("top_page", string().maxLength(100)), (String) null)
    ).map(ApplicationCreate::new)::decode;

    public static final JsonDecoder<ApplicationUpdate> APPLICATION_UPDATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank()),
            field("virtual_path", string().nonBlank().maxLength(100)),
            field("pass_to", string().nonBlank().maxLength(100)),
            withDefault(field("top_page", string().maxLength(100)), (String) null)
    ).map(ApplicationUpdate::new)::decode;

    // ===== Group =====

    public static final JsonDecoder<GroupCreate> GROUP_CREATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank())
    ).map(GroupCreate::new)::decode;

    public static final JsonDecoder<GroupUpdate> GROUP_UPDATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank()),
            withDefault(field("users", list(string())), (List<String>) null)
    ).map(GroupUpdate::new)::decode;

    // ===== Role =====

    public static final JsonDecoder<RoleCreate> ROLE_CREATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank())
    ).map(RoleCreate::new)::decode;

    public static final JsonDecoder<RoleUpdate> ROLE_UPDATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank())
    ).map(RoleUpdate::new)::decode;

    // ===== Permission =====

    public static final JsonDecoder<PermissionCreate> PERMISSION_CREATE = combine(
            field("name", PERMISSION_NAME),
            field("description", string().nonBlank())
    ).map(PermissionCreate::new)::decode;

    public static final JsonDecoder<PermissionUpdate> PERMISSION_UPDATE = combine(
            field("name", PERMISSION_NAME),
            field("description", string().nonBlank())
    ).map(PermissionUpdate::new)::decode;

    // ===== Realm =====

    public static final JsonDecoder<RealmCreate> REALM_CREATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank()),
            field("url", string().nonBlank())
    ).map(RealmCreate::new)::decode;

    public static final JsonDecoder<RealmUpdate> REALM_UPDATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank())
    ).map(RealmUpdate::new)::decode;

    // ===== Invitation =====

    public static final JsonDecoder<InvitationCreate> INVITATION_CREATE = combine(
            field("email", string().email()),
            withDefault(field("groups", list(field("id", long_()).map(IdObject::new))), List.of())
    ).map(InvitationCreate::new)::decode;

    // ===== Password Sign In =====

    public static final JsonDecoder<PasswordSignIn> PASSWORD_SIGN_IN = combine(
            field("account", WORD_NAME),
            field("password", PASSWORD),
            withDefault(field("one_time_password", string().maxLength(100)), (String) null)
    ).map(PasswordSignIn::new)::decode;

    // ===== Password Credential =====

    public static final JsonDecoder<PasswordCredentialCreate> PASSWORD_CREDENTIAL_CREATE = combine(
            field("account", WORD_NAME),
            field("password", PASSWORD),
            withDefault(field("initial", bool()), true)
    ).map(PasswordCredentialCreate::new)::decode;

    public static final JsonDecoder<PasswordCredentialUpdate> PASSWORD_CREDENTIAL_UPDATE = combine(
            withDefault(field("account", string()), (String) null),
            field("old_password", PASSWORD),
            field("new_password", PASSWORD)
    ).map(PasswordCredentialUpdate::new)::decode;

    public static final JsonDecoder<PasswordCredentialDelete> PASSWORD_CREDENTIAL_DELETE = combine(
            field("account", WORD_NAME),
            field("password", PASSWORD)
    ).map(PasswordCredentialDelete::new)::decode;

    // ===== Password Reset Challenge =====

    public static final JsonDecoder<PasswordResetChallengeCreate> PASSWORD_RESET_CHALLENGE_CREATE =
            field("account", WORD_NAME)
                    .map(PasswordResetChallengeCreate::new)::decode;

    // ===== Password Reset =====

    public static final JsonDecoder<PasswordReset> PASSWORD_RESET =
            field("code", string()).map(PasswordReset::new)::decode;

    // ===== Token Refresh =====

    public static final JsonDecoder<TokenRefresh> TOKEN_REFRESH =
            field("session_id", string().nonBlank())
                    .map(TokenRefresh::new)::decode;

    // ===== Role Permissions =====
    public static final JsonDecoder<List<String>> ROLE_PERMISSIONS = list(string())::decode;

    // ===== Group Users =====
    public static final JsonDecoder<List<String>> GROUP_USERS = list(string())::decode;

    // ===== Assignment =====

    private static final JsonDecoder<AssignmentIdObject> ID_OBJECT = combine(
            withDefault(field("id", long_()), (Long) null),
            withDefault(field("name", string()), (String) null)
    ).map(AssignmentIdObject::new)::decode;

    public static final JsonDecoder<List<AssignmentItem>> ASSIGNMENTS = list(
            combine(
                    field("group", ID_OBJECT),
                    field("role", ID_OBJECT),
                    field("realm", ID_OBJECT)
            ).map(AssignmentItem::new)
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

    public static JsonDecoder<List<ResolvedAssignment>> assignments(AssignmentRepository repo) {
        return list(
                combine(
                        field("group", resolvedId(repo, "groups", "group_id")),
                        field("role", resolvedId(repo, "roles", "role_id")),
                        field("realm", resolvedId(repo, "realms", "realm_id"))
                ).map(ResolvedAssignment::new)
        )::decode;
    }

    // ===== OIDC Provider =====

    private record OidcProviderBody(String name, String clientId, String clientSecret, String scope,
                                     String responseType, String authorizationEndpoint, String tokenEndpoint,
                                     String tokenEndpointAuthMethod, String redirectUri, String jwksUri,
                                     String issuer, boolean pkceEnabled) {}

    private static final Decoder<JsonNode, OidcProviderBody> OIDC_PROVIDER_BODY = combine(
            field("name", WORD_NAME),
            field("client_id", string().nonBlank().maxLength(255)),
            field("client_secret", string().nonBlank().maxLength(255)),
            field("scope", string().nonBlank().maxLength(255)),
            field("response_type", string().nonBlank().maxLength(16)),
            field("authorization_endpoint", string().nonBlank().maxLength(255)),
            withDefault(field("token_endpoint", string().maxLength(255)), (String) null),
            field("token_endpoint_auth_method", string().nonBlank()),
            field("redirect_uri", string().nonBlank().maxLength(255)),
            withDefault(field("jwks_uri", string().maxLength(512)), (String) null),
            withDefault(field("issuer", string().maxLength(512)), (String) null),
            withDefault(field("pkce_enabled", bool()), false)
    ).map(OidcProviderBody::new);

    public static final JsonDecoder<OidcProviderCreate> OIDC_PROVIDER_CREATE =
            OIDC_PROVIDER_BODY.map(b -> new OidcProviderCreate(b.name(), b.clientId(), b.clientSecret(),
                    b.scope(), b.responseType(), b.authorizationEndpoint(), b.tokenEndpoint(),
                    b.tokenEndpointAuthMethod(), b.redirectUri(), b.jwksUri(), b.issuer(), b.pkceEnabled()))::decode;

    public static final JsonDecoder<OidcProviderUpdate> OIDC_PROVIDER_UPDATE =
            OIDC_PROVIDER_BODY.map(b -> new OidcProviderUpdate(b.name(), b.clientId(), b.clientSecret(),
                    b.scope(), b.responseType(), b.authorizationEndpoint(), b.tokenEndpoint(),
                    b.tokenEndpointAuthMethod(), b.redirectUri(), b.jwksUri(), b.issuer(), b.pkceEnabled()))::decode;

    // ===== OIDC Application =====

    public static final JsonDecoder<OidcApplicationCreate> OIDC_APPLICATION_CREATE = combine(
            field("name", WORD_NAME),
            field("grant_types", list(string())),
            withDefault(field("home_uri", httpUrl(2048)), (String) null),
            withDefault(field("callback_uri", httpUrl(2048)), (String) null),
            withDefault(field("description", string().maxLength(255)), (String) null),
            withDefault(field("backchannel_logout_uri", httpUrl(2048)), (String) null),
            withDefault(field("frontchannel_logout_uri", httpUrl(2048)), (String) null),
            withDefault(field("permissions", list(string())), List.of())
    ).<OidcApplicationCreate>flatMap((name, gt, hu, cu, desc, bcu, fcu, perms) ->
            validateOidcAppGrantTypes(gt, cu, hu).map(v ->
                    new OidcApplicationCreate(name, gt, hu, cu, desc, bcu, fcu, perms)))::decode;

    public static final JsonDecoder<OidcApplicationUpdate> OIDC_APPLICATION_UPDATE = combine(
            field("name", WORD_NAME),
            field("grant_types", list(string())),
            optionalNullableField("home_uri", httpUrl(2048)),
            optionalNullableField("callback_uri", httpUrl(2048)),
            optionalNullableField("description", string().maxLength(255)),
            optionalNullableField("backchannel_logout_uri", httpUrl(2048)),
            optionalNullableField("frontchannel_logout_uri", httpUrl(2048)),
            withDefault(field("permissions", list(string())), List.of())
    ).<OidcApplicationUpdate>flatMap((name, gt, hu, cu, desc, bcu, fcu, perms) ->
            validateOidcAppGrantTypes(gt, presenceToNullable(cu), presenceToNullable(hu)).map(v ->
                    new OidcApplicationUpdate(name, gt, hu, cu, desc, bcu, fcu, perms)))::decode;

    // ===== Sign Up =====

    public static final JsonDecoder<SignUp> SIGN_UP = combine(
            field("account", WORD_NAME),
            withDefault(field("code", string()), (String) null),
            withDefault(field("enable_password_credential", bool()), true)
    ).map(SignUp::new)::decode;

    // ===== User Create (admin) =====

    public static final JsonDecoder<UserCreate> USER_CREATE =
            field("account", WORD_NAME)
                    .map(UserCreate::new)::decode;

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

    public static final JsonDecoder<WebAuthnRegister> WEBAUTHN_REGISTER = combine(
            field("registration_response_json", string().nonBlank()),
            withDefault(field("credential_name", string().maxLength(100)), (String) null)
    ).map(WebAuthnRegister::new)::decode;

    public static final JsonDecoder<WebAuthnAuthenticate> WEBAUTHN_AUTHENTICATE =
            field("authentication_response_json", string().nonBlank())
                    .map(WebAuthnAuthenticate::new)::decode;

    public static final JsonDecoder<WebAuthnSignInOptions> WEBAUTHN_SIGN_IN_OPTIONS =
            withDefault(field("account", string().maxLength(100)), (String) null)
                    .map(WebAuthnSignInOptions::new)::decode;

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
