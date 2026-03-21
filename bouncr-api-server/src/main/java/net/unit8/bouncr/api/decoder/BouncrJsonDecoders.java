package net.unit8.bouncr.api.decoder;

import kotowari.restful.data.Problem;
import net.unit8.raoh.Issues;
import net.unit8.raoh.json.JsonDecoder;

import java.util.List;
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

    // ===== Application =====
    public record ApplicationCreate(String name, String description, String virtualPath, String passTo, String topPage) {}
    public static final JsonDecoder<ApplicationCreate> APPLICATION_CREATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
            field("description", string().nonBlank()),
            field("virtual_path", string().nonBlank().maxLength(100)),
            field("pass_to", string().nonBlank().maxLength(100)),
            optionalField("top_page", string().maxLength(100))
    ).map((name, desc, vp, pt, tp) -> new ApplicationCreate(name, desc, vp, pt, tp.orElse(null)))::decode;

    public record ApplicationUpdate(String name, String description, String virtualPath, String passTo, String topPage) {}
    public static final JsonDecoder<ApplicationUpdate> APPLICATION_UPDATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
            field("description", string().nonBlank()),
            field("virtual_path", string().nonBlank().maxLength(100)),
            field("pass_to", string().nonBlank().maxLength(100)),
            optionalField("top_page", string().maxLength(100))
    ).map((name, desc, vp, pt, tp) -> new ApplicationUpdate(name, desc, vp, pt, tp.orElse(null)))::decode;

    // ===== Group =====
    public record GroupCreate(String name, String description) {}
    public static final JsonDecoder<GroupCreate> GROUP_CREATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
            field("description", string().nonBlank())
    ).map(GroupCreate::new)::decode;

    public record GroupUpdate(String name, String description, List<String> users) {}
    public static final JsonDecoder<GroupUpdate> GROUP_UPDATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
            field("description", string().nonBlank()),
            optionalField("users", list(string()))
    ).map((name, desc, users) -> new GroupUpdate(name, desc, users.orElse(null)))::decode;

    // ===== Role =====
    public record RoleCreate(String name, String description) {}
    public static final JsonDecoder<RoleCreate> ROLE_CREATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
            field("description", string().nonBlank())
    ).map(RoleCreate::new)::decode;

    public record RoleUpdate(String name, String description) {}
    public static final JsonDecoder<RoleUpdate> ROLE_UPDATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
            field("description", string().nonBlank())
    ).map(RoleUpdate::new)::decode;

    // ===== Permission =====
    public record PermissionCreate(String name, String description) {}
    public static final JsonDecoder<PermissionCreate> PERMISSION_CREATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(PERMISSION_PATTERN)),
            field("description", string().nonBlank())
    ).map(PermissionCreate::new)::decode;

    public record PermissionUpdate(String name, String description) {}
    public static final JsonDecoder<PermissionUpdate> PERMISSION_UPDATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(PERMISSION_PATTERN)),
            field("description", string().nonBlank())
    ).map(PermissionUpdate::new)::decode;

    // ===== Realm =====
    public record RealmCreate(String name, String description, String url) {}
    public static final JsonDecoder<RealmCreate> REALM_CREATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
            field("description", string().nonBlank()),
            field("url", string().nonBlank())
    ).map(RealmCreate::new)::decode;

    public record RealmUpdate(String name, String description) {}
    public static final JsonDecoder<RealmUpdate> REALM_UPDATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
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
            field("account", string().nonBlank().maxLength(100)),
            field("password", string().nonBlank().maxLength(300)),
            optionalField("one_time_password", string().maxLength(100))
    ).map((acc, pwd, otp) -> new PasswordSignIn(acc, pwd, otp.orElse(null)))::decode;

    // ===== Password Credential =====
    public record PasswordCredentialCreate(String account, String password, boolean initial) {}
    public static final JsonDecoder<PasswordCredentialCreate> PASSWORD_CREDENTIAL_CREATE = combine(
            field("account", string().nonBlank().maxLength(100)),
            field("password", string().nonBlank()),
            optionalField("initial", bool())
    ).map((acc, pwd, initial) -> new PasswordCredentialCreate(acc, pwd, initial.orElse(true)))::decode;

    public record PasswordCredentialUpdate(String account, String oldPassword, String newPassword) {}
    public static final JsonDecoder<PasswordCredentialUpdate> PASSWORD_CREDENTIAL_UPDATE = combine(
            optionalField("account", string()),
            field("old_password", string().nonBlank()),
            field("new_password", string().nonBlank())
    ).map((acc, old, new_) -> new PasswordCredentialUpdate(acc.orElse(null), old, new_))::decode;

    public record PasswordCredentialDelete(String account, String password) {}
    public static final JsonDecoder<PasswordCredentialDelete> PASSWORD_CREDENTIAL_DELETE = combine(
            field("account", string().nonBlank().maxLength(100)),
            field("password", string().nonBlank())
    ).map(PasswordCredentialDelete::new)::decode;

    // ===== Password Reset Challenge =====
    public record PasswordResetChallengeCreate(String account) {}
    public static final JsonDecoder<PasswordResetChallengeCreate> PASSWORD_RESET_CHALLENGE_CREATE =
            field("account", string().nonBlank().maxLength(100).pattern(WORD_PATTERN))
                    .map(PasswordResetChallengeCreate::new)::decode;

    // ===== Password Reset =====
    public record PasswordReset(String code) {}
    public static final JsonDecoder<PasswordReset> PASSWORD_RESET =
            field("code", string()).map(PasswordReset::new)::decode;

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

    // ===== OIDC Provider =====
    public record OidcProviderCreate(String name, String clientId, String clientSecret, String scope,
                                      String responseType, String authorizationEndpoint, String tokenEndpoint,
                                      String tokenEndpointAuthMethod, String redirectUri, String jwksUri,
                                      String issuer, boolean pkceEnabled) {}
    public static final JsonDecoder<OidcProviderCreate> OIDC_PROVIDER_CREATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
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
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
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
    public record OidcApplicationCreate(String name, String homeUrl, String callbackUrl, String description, List<String> permissions) {}
    public static final JsonDecoder<OidcApplicationCreate> OIDC_APPLICATION_CREATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
            field("home_url", string().nonBlank()),
            field("callback_url", string().nonBlank()),
            field("description", string().nonBlank()),
            optionalField("permissions", list(string()))
    ).map((name, hu, cu, desc, perms) -> new OidcApplicationCreate(name, hu, cu, desc, perms.orElse(List.of())))::decode;

    public record OidcApplicationUpdate(String name, String homeUrl, String callbackUrl, String description, List<String> permissions) {}
    public static final JsonDecoder<OidcApplicationUpdate> OIDC_APPLICATION_UPDATE = combine(
            field("name", string().nonBlank().maxLength(100).pattern(WORD_PATTERN)),
            field("home_url", string().nonBlank()),
            field("callback_url", string().nonBlank()),
            field("description", string().nonBlank()),
            optionalField("permissions", list(string()))
    ).map((name, hu, cu, desc, perms) -> new OidcApplicationUpdate(name, hu, cu, desc, perms.orElse(List.of())))::decode;

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
}
