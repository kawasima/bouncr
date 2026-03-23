package net.unit8.bouncr.api.decoder;

import kotowari.restful.data.Problem;
import net.unit8.bouncr.api.repository.AssignmentRepository;
import net.unit8.bouncr.api.repository.UserProfileFieldRepository;
import net.unit8.bouncr.data.UserProfile;
import net.unit8.bouncr.data.UserProfileField;
import net.unit8.raoh.Decoder;
import net.unit8.raoh.Issue;
import net.unit8.raoh.Issues;
import net.unit8.raoh.Path;
import net.unit8.raoh.Result;
import net.unit8.raoh.json.JsonDecoder;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static net.unit8.raoh.json.JsonDecoders.*;

/**
 * Centralized raoh-json decoders for all API request types.
 * Replaces BeansValidator + boundary classes.
 */
public final class BouncrJsonDecoders {
    private BouncrJsonDecoders() {}

    public static Problem toProblem(Issues issues) {
        return Problem.fromViolationList(issues.asList().stream()
                .map(i -> new Problem.Violation(i.path().toString(), i.code(), i.message()))
                .toList());
    }

    private static final Pattern WORD_PATTERN = Pattern.compile("^\\w+$");
    private static final Pattern PERMISSION_PATTERN = Pattern.compile("^[\\w:]+$");
    private static final Decoder<JsonNode, String> WORD_NAME = string().nonBlank().maxLength(100).pattern(WORD_PATTERN);
    private static final Decoder<JsonNode, String> PERMISSION_NAME = string().nonBlank().maxLength(100).pattern(PERMISSION_PATTERN);
    private static final Decoder<JsonNode, String> PASSWORD = string().nonBlank().maxLength(300);

    // ===== Application =====
    public record ApplicationCreate(String name, String description, String virtualPath, String passTo, String topPage) {}
    public static final JsonDecoder<ApplicationCreate> APPLICATION_CREATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank()),
            field("virtual_path", string().nonBlank().maxLength(100)),
            field("pass_to", string().nonBlank().maxLength(100)),
            optionalField("top_page", string().maxLength(100))
    ).map((name, desc, vp, pt, tp) -> new ApplicationCreate(name, desc, vp, pt, tp.orElse(null)))::decode;

    public record ApplicationUpdate(String name, String description, String virtualPath, String passTo, String topPage) {}
    public static final JsonDecoder<ApplicationUpdate> APPLICATION_UPDATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank()),
            field("virtual_path", string().nonBlank().maxLength(100)),
            field("pass_to", string().nonBlank().maxLength(100)),
            optionalField("top_page", string().maxLength(100))
    ).map((name, desc, vp, pt, tp) -> new ApplicationUpdate(name, desc, vp, pt, tp.orElse(null)))::decode;

    // ===== Group =====
    public record GroupCreate(String name, String description) {}
    public static final JsonDecoder<GroupCreate> GROUP_CREATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank())
    ).map(GroupCreate::new)::decode;

    public record GroupUpdate(String name, String description, List<String> users) {}
    public static final JsonDecoder<GroupUpdate> GROUP_UPDATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank()),
            optionalField("users", list(string()))
    ).map((name, desc, users) -> new GroupUpdate(name, desc, users.orElse(null)))::decode;

    // ===== Role =====
    public record RoleCreate(String name, String description) {}
    public static final JsonDecoder<RoleCreate> ROLE_CREATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank())
    ).map(RoleCreate::new)::decode;

    public record RoleUpdate(String name, String description) {}
    public static final JsonDecoder<RoleUpdate> ROLE_UPDATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank())
    ).map(RoleUpdate::new)::decode;

    // ===== Permission =====
    public record PermissionCreate(String name, String description) {}
    public static final JsonDecoder<PermissionCreate> PERMISSION_CREATE = combine(
            field("name", PERMISSION_NAME),
            field("description", string().nonBlank())
    ).map(PermissionCreate::new)::decode;

    public record PermissionUpdate(String name, String description) {}
    public static final JsonDecoder<PermissionUpdate> PERMISSION_UPDATE = combine(
            field("name", PERMISSION_NAME),
            field("description", string().nonBlank())
    ).map(PermissionUpdate::new)::decode;

    // ===== Realm =====
    public record RealmCreate(String name, String description, String url) {}
    public static final JsonDecoder<RealmCreate> REALM_CREATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank()),
            field("url", string().nonBlank())
    ).map(RealmCreate::new)::decode;

    public record RealmUpdate(String name, String description) {}
    public static final JsonDecoder<RealmUpdate> REALM_UPDATE = combine(
            field("name", WORD_NAME),
            field("description", string().nonBlank())
    ).map(RealmUpdate::new)::decode;

    // ===== Invitation =====
    public record IdObject(Long id) {}
    public record InvitationCreate(String email, List<IdObject> groups) {}
    public static final JsonDecoder<InvitationCreate> INVITATION_CREATE = combine(
            field("email", string().email()),
            optionalField("groups", list(
                    field("id", long_()).map(IdObject::new)
            ))
    ).map((email, groups) -> new InvitationCreate(email, groups.orElse(List.of())))::decode;

    // ===== Password Sign In =====
    public record PasswordSignIn(String account, String password, String oneTimePassword) {}
    public static final JsonDecoder<PasswordSignIn> PASSWORD_SIGN_IN = combine(
            field("account", WORD_NAME),
            field("password", PASSWORD),
            optionalField("one_time_password", string().maxLength(100))
    ).map((acc, pwd, otp) -> new PasswordSignIn(acc, pwd, otp.orElse(null)))::decode;

    // ===== Password Credential =====
    public record PasswordCredentialCreate(String account, String password, boolean initial) {}
    public static final JsonDecoder<PasswordCredentialCreate> PASSWORD_CREDENTIAL_CREATE = combine(
            field("account", WORD_NAME),
            field("password", PASSWORD),
            optionalField("initial", bool())
    ).map((acc, pwd, initial) -> new PasswordCredentialCreate(acc, pwd, initial.orElse(true)))::decode;

    public record PasswordCredentialUpdate(String account, String oldPassword, String newPassword) {}
    public static final JsonDecoder<PasswordCredentialUpdate> PASSWORD_CREDENTIAL_UPDATE = combine(
            optionalField("account", string()),
            field("old_password", PASSWORD),
            field("new_password", PASSWORD)
    ).map((acc, old, new_) -> new PasswordCredentialUpdate(acc.orElse(null), old, new_))::decode;

    public record PasswordCredentialDelete(String account, String password) {}
    public static final JsonDecoder<PasswordCredentialDelete> PASSWORD_CREDENTIAL_DELETE = combine(
            field("account", WORD_NAME),
            field("password", PASSWORD)
    ).map(PasswordCredentialDelete::new)::decode;

    // ===== Password Reset Challenge =====
    public record PasswordResetChallengeCreate(String account) {}
    public static final JsonDecoder<PasswordResetChallengeCreate> PASSWORD_RESET_CHALLENGE_CREATE =
            field("account", WORD_NAME)
                    .map(PasswordResetChallengeCreate::new)::decode;

    // ===== Password Reset =====
    public record PasswordReset(String code) {}
    public static final JsonDecoder<PasswordReset> PASSWORD_RESET =
            field("code", string()).map(PasswordReset::new)::decode;

    // ===== Token Refresh =====
    public record TokenRefresh(String sessionId) {}
    public static final JsonDecoder<TokenRefresh> TOKEN_REFRESH =
            field("session_id", string().nonBlank())
                    .map(TokenRefresh::new)::decode;

    // ===== Role Permissions =====
    public static final JsonDecoder<List<String>> ROLE_PERMISSIONS = list(string())::decode;

    // ===== Group Users =====
    public static final JsonDecoder<List<String>> GROUP_USERS = list(string())::decode;

    // ===== Assignment =====
    public record AssignmentIdObject(Long id, String name) {}
    public record AssignmentItem(AssignmentIdObject group, AssignmentIdObject role, AssignmentIdObject realm) {}

    private static final JsonDecoder<AssignmentIdObject> ID_OBJECT = combine(
            optionalField("id", long_()),
            optionalField("name", string())
    ).map((id, name) -> new AssignmentIdObject(id.orElse(null), name.orElse(null)))::decode;

    public static final JsonDecoder<List<AssignmentItem>> ASSIGNMENTS = list(
            combine(
                    field("group", ID_OBJECT),
                    field("role", ID_OBJECT),
                    field("realm", ID_OBJECT)
            ).map(AssignmentItem::new)
    )::decode;

    public record ResolvedAssignment(Long groupId, Long roleId, Long realmId) {}

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
    public record OidcProviderCreate(String name, String clientId, String clientSecret, String scope,
                                      String responseType, String authorizationEndpoint, String tokenEndpoint,
                                      String tokenEndpointAuthMethod, String redirectUri, String jwksUri,
                                      String issuer, boolean pkceEnabled) {}
    public static final JsonDecoder<OidcProviderCreate> OIDC_PROVIDER_CREATE = combine(
            field("name", WORD_NAME),
            field("client_id", string().nonBlank().maxLength(255)),
            field("client_secret", string().nonBlank().maxLength(255)),
            field("scope", string().nonBlank().maxLength(255)),
            field("response_type", string().nonBlank().maxLength(16)),
            field("authorization_endpoint", string().nonBlank().maxLength(255)),
            optionalField("token_endpoint", string().maxLength(255)),
            field("token_endpoint_auth_method", string().nonBlank()),
            field("redirect_uri", string().nonBlank().maxLength(255)),
            optionalField("jwks_uri", string().maxLength(512)),
            optionalField("issuer", string().maxLength(512)),
            optionalField("pkce_enabled", bool())
    ).map((name, cid, cs, scope, rt, ae, te, team, ru, jwks, iss, pkce) ->
            new OidcProviderCreate(name, cid, cs, scope, rt, ae, te.orElse(null), team, ru,
                    jwks.orElse(null), iss.orElse(null), pkce.orElse(false)))::decode;

    public record OidcProviderUpdate(String name, String clientId, String clientSecret, String scope,
                                      String responseType, String authorizationEndpoint, String tokenEndpoint,
                                      String tokenEndpointAuthMethod, String redirectUri, String jwksUri,
                                      String issuer, boolean pkceEnabled) {}
    public static final JsonDecoder<OidcProviderUpdate> OIDC_PROVIDER_UPDATE = combine(
            field("name", WORD_NAME),
            field("client_id", string().nonBlank().maxLength(256)),
            field("client_secret", string().nonBlank().maxLength(256)),
            field("scope", string().nonBlank().maxLength(256)),
            field("response_type", string().nonBlank().maxLength(16)),
            field("authorization_endpoint", string().nonBlank().maxLength(256)),
            optionalField("token_endpoint", string().maxLength(256)),
            field("token_endpoint_auth_method", string().nonBlank()),
            field("redirect_uri", string().nonBlank().maxLength(255)),
            optionalField("jwks_uri", string().maxLength(512)),
            optionalField("issuer", string().maxLength(512)),
            optionalField("pkce_enabled", bool())
    ).map((name, cid, cs, scope, rt, ae, te, team, ru, jwks, iss, pkce) ->
            new OidcProviderUpdate(name, cid, cs, scope, rt, ae, te.orElse(null), team, ru,
                    jwks.orElse(null), iss.orElse(null), pkce.orElse(false)))::decode;

    // ===== OIDC Application =====
    public record OidcApplicationCreate(String name, List<String> grantTypes,
                                        String homeUrl, String callbackUrl, String description,
                                        String backchannelLogoutUri, String frontchannelLogoutUri,
                                        List<String> permissions) {}
    public static final JsonDecoder<OidcApplicationCreate> OIDC_APPLICATION_CREATE = (in, path) -> combine(
            field("name", WORD_NAME),
            field("grant_types", list(string())),
            optionalField("home_url", string().maxLength(2048)),
            optionalField("callback_url", string().maxLength(2048)),
            optionalField("description", string().maxLength(255)),
            optionalField("backchannel_logout_uri", string().maxLength(2048)),
            optionalField("frontchannel_logout_uri", string().maxLength(2048)),
            optionalField("permissions", list(string()))
    ).map((name, gt, hu, cu, desc, bcu, fcu, perms) ->
            new OidcApplicationCreate(name, gt,
                    blankToNull(hu.orElse(null)), blankToNull(cu.orElse(null)),
                    blankToNull(desc.orElse(null)),
                    blankToNull(bcu.orElse(null)), blankToNull(fcu.orElse(null)),
                    perms.orElse(List.of())))
    .<OidcApplicationCreate>flatMap(app -> validateOidcAppGrantTypes(
            app.grantTypes(), app.callbackUrl(), app.homeUrl()).map(v -> app))
    .decode(in, path);

    public record OidcApplicationUpdate(String name, List<String> grantTypes,
                                        String homeUrl, String callbackUrl, String description,
                                        String backchannelLogoutUri, String frontchannelLogoutUri,
                                        List<String> permissions,
                                        boolean hasBackchannelLogoutUri, boolean hasFrontchannelLogoutUri) {}
    public static final JsonDecoder<OidcApplicationUpdate> OIDC_APPLICATION_UPDATE = (in, path) -> combine(
            field("name", WORD_NAME),
            field("grant_types", list(string())),
            optionalField("home_url", string().maxLength(2048)),
            optionalField("callback_url", string().maxLength(2048)),
            optionalField("description", string().maxLength(255)),
            optionalField("backchannel_logout_uri", string().maxLength(2048)),
            optionalField("frontchannel_logout_uri", string().maxLength(2048)),
            optionalField("permissions", list(string()))
    ).map((name, gt, hu, cu, desc, bcu, fcu, perms) -> new OidcApplicationUpdate(
            name, gt,
            blankToNull(hu.orElse(null)), blankToNull(cu.orElse(null)),
            blankToNull(desc.orElse(null)),
            blankToNull(bcu.orElse(null)), blankToNull(fcu.orElse(null)),
            perms.orElse(List.of()),
            in.has("backchannel_logout_uri"), in.has("frontchannel_logout_uri")))
    .<OidcApplicationUpdate>flatMap(app -> validateOidcAppGrantTypes(
            app.grantTypes(), app.callbackUrl(), app.homeUrl()).map(v -> app))
    .decode(in, path);

    // ===== Sign Up =====
    public record SignUp(String account, String code, boolean enablePasswordCredential) {}
    public static final JsonDecoder<SignUp> SIGN_UP = combine(
            field("account", WORD_NAME),
            optionalField("code", string()),
            optionalField("enable_password_credential", bool())
    ).map((acc, code, enable) -> new SignUp(acc, code.orElse(null), enable.orElse(true)))::decode;

    // ===== User Create (admin) =====
    public record UserCreate(String account) {}
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
    public record WebAuthnRegister(String registrationResponseJSON, String credentialName) {}
    public static final JsonDecoder<WebAuthnRegister> WEBAUTHN_REGISTER = combine(
            field("registration_response_json", string().nonBlank()),
            optionalField("credential_name", string().maxLength(100))
    ).map((json, name) -> new WebAuthnRegister(json, name.orElse(null)))::decode;

    public record WebAuthnAuthenticate(String authenticationResponseJSON) {}
    public static final JsonDecoder<WebAuthnAuthenticate> WEBAUTHN_AUTHENTICATE =
            field("authentication_response_json", string().nonBlank())
                    .map(WebAuthnAuthenticate::new)::decode;

    public record WebAuthnSignInOptions(String account) {}
    public static final JsonDecoder<WebAuthnSignInOptions> WEBAUTHN_SIGN_IN_OPTIONS =
            optionalField("account", string().maxLength(100))
                    .map(acc -> new WebAuthnSignInOptions(acc.orElse(null)))::decode;

    // ===== OIDC Application helpers =====

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
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
            return Result.fail(Path.ROOT.append("callback_url"), "required",
                    "callback_url is required when authorization_code grant is enabled");
        }
        // Validate URLs are absolute http(s)
        if (callbackUrl != null) {
            Result<Void> r = validateHttpUrl(callbackUrl, "callback_url");
            if (r instanceof net.unit8.raoh.Err) return r;
        }
        if (homeUrl != null) {
            Result<Void> r = validateHttpUrl(homeUrl, "home_url");
            if (r instanceof net.unit8.raoh.Err) return r;
        }
        return Result.ok(null);
    }

    private static Result<Void> validateHttpUrl(String url, String fieldName) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            if (!uri.isAbsolute() || (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))) {
                return Result.fail(Path.ROOT.append(fieldName), "invalid", "must be an absolute http or https URL");
            }
        } catch (IllegalArgumentException e) {
            return Result.fail(Path.ROOT.append(fieldName), "invalid", "not a valid URI");
        }
        return Result.ok(null);
    }
}
