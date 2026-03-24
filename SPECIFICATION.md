# Specification model

```specification
// ===== Primitive values =====
data UserId = Long
data GroupId = Long
data ApplicationId = Long
data RoleId = Long
data PermissionId = Long
data RealmId = Long
// 8 random alphanumeric characters
data InvitationCode = String
// UUID, used as Redis key for session lookup
data Token = String
// HS256-signed, injected as x-bouncr-credential header
data JWT = String

// Domain primitive types (validated at the boundary, carried as types)
// ^\w+$, max 100 chars — account, group/role/realm/application names
data WordName = String
// ^[\w:]+$, max 100 chars — permission names (colon-separated namespaces)
data PermissionName = String
// RFC 5322 email address
data Email = String

// ===== User =====
// account: case-insensitive unique string (accountLower stored separately for lookup)
// User has no email column — email is stored as a UserProfileValue
data User = UserId AND account AND List<Group> AND List<UserProfileValue> AND UserLock? AND PasswordCredential? AND OtpKey? AND List<OidcUser>

// ===== User profile =====
// isIdentity=true: this field is used for user lookup (e.g. email address)
// needsVerification=true: value change requires email verification before it takes effect
data UserProfileField = UserProfileFieldId AND name AND jsonName AND isRequired AND isIdentity AND needsVerification AND regularExpression? AND maxLength? AND minLength?

// The actual stored value for a profile field; serialized under jsonName in API responses
data UserProfileValue = UserProfileField AND User AND value

// Pending verification record created when a needsVerification field is changed
data UserProfileVerification = UserProfileValue AND code AND expiresAt

// ===== Password credential =====
// initial=true: temporary password, user must change on first login
// hashing algorithm is PBKDF2 / bcrypt (not stored in the entity)
data PasswordCredential = User AND passwordHash AND salt AND initial AND createdAt

// ===== OTP =====
// TOTP shared secret for two-factor authentication
data OtpKey = User AND key

// ===== Lock =====
// lockLevel: LockLevel enum
// Created after repeated authentication failures; removed by admin unlock
data UserLock = User AND lockLevel AND lockedAt

// ===== Session =====
// Persisted in DB; Token is also written to Redis (msgpack) with permissions for fast lookup
data UserSession = Token AND User AND remoteAddress AND userAgent AND createdAt

// ===== Groups and roles =====
// Membership is stored in the memberships join table (many-to-many with User)
data Group = GroupId AND name AND description? AND List<User>

// Permissions are granted to a Role; Roles are assigned to a Group within a Realm
data Role = RoleId AND name AND description? AND List<Permission>

data Permission = PermissionId AND name AND description?

// ===== Access control =====
// virtualPath: URL prefix exposed to clients (e.g. /myapp)
// passTo: upstream backend URL for Envoy routing (e.g. http://backend:8080)
data Application = ApplicationId AND name AND description? AND virtualPath AND passTo AND topPage? AND List<Realm>

// url: URL pattern suffix relative to Application.virtualPath
// pathPattern (transient, not persisted): compiled as ^{virtualPath}($|/{url})
data Realm = RealmId AND name AND url AND description? AND Application AND List<Assignment>

// Grants a Role to a Group scoped to a Realm
// Effective permissions for a request: User -> memberships -> Groups
//   -> Assignments matching resolved Realm -> Roles -> Permissions
data Assignment = Group AND Role AND Realm

// ===== OIDC Provider =====
// OpenID Provider Metadata (RFC 8414) — endpoints and identity published by the provider
data OidcProviderMetadata = authorizationEndpoint AND tokenEndpoint AND jwksUri(URI)? AND issuer?

// Client registration settings for authenticating against the provider
data OidcProviderClientConfig = ClientCredentials AND scope AND responseType AND tokenEndpointAuthMethod AND redirectUri(URI) AND pkceEnabled

data OidcProvider = OidcProviderId AND name AND OidcProviderMetadata AND OidcProviderClientConfig

// Links an external OIDC identity (issuer + sub) to a Bouncr User
data OidcUser = OidcProvider AND User AND oidcSub

// ===== OIDC Application (Client) =====
// OAuth2 client credentials — always used as an inseparable pair
data ClientCredentials = clientId AND clientSecret

// JWT signing key pair for OIDC client applications
data SigningKeyPair = privateKey AND publicKey

// OIDC client registration metadata (RFC 7591)
// homeUri: Bouncr extension — landing page for the application
// callbackUri: redirect URI for authorization code flow (RFC 6749)
data OidcClientMetadata = homeUri(URI)? AND callbackUri(URI)? AND backchannelLogoutUri(URI)? AND frontchannelLogoutUri(URI)? AND Set<GrantType>

// Invariant: permissions granted to an OidcApplication must be a subset
// of the operating user's own permissions (enforced at create and update)
data OidcApplication = OidcApplicationId AND name AND ClientCredentials AND SigningKeyPair? AND OidcClientMetadata AND description? AND List<Permission>

// ===== Invitation =====
data Invitation = InvitationCode AND email AND invitedAt AND List<GroupInvitation> AND List<OidcInvitation>

// Records which groups the invited user will be added to upon completing registration
data GroupInvitation = Invitation AND Group

// Records which OIDC provider the invited user must authenticate through
data OidcInvitation = Invitation AND OidcProvider

// ===== OAuth2 protocol types =====
// OAuth2 authorization request parameters (RFC 6749 §4.1.1)
data AuthorizeRequest = responseType AND clientId AND redirectUri AND Scope AND state? AND nonce? AND PkceChallenge?

// Sealed: each grant type carries only its relevant parameters
data TokenRequest = AuthorizationCodeGrant | RefreshTokenGrant | ClientCredentialsGrant

// Immutable set of OAuth2 scope values, parsed from space-separated string
data Scope = Set<String>

// PKCE S256 challenge with constant-time verification
data PkceChallenge = challenge AND method

// ===== Password reset =====
data PasswordResetChallenge = User AND code AND expiresAt

// ===== Audit log =====
// actionType: Action enum (SIGN_IN, SIGN_OUT, CHANGE_PASSWORD, etc.)
data UserAction = User AND actionType AND createdAt AND remoteAddress? AND description?

// ===== mTLS certificate =====
data Cert = serial AND commonName AND issuerCommonName AND expiresAt

// ===== Behaviors =====

// --- Authentication ---
// Failure increments a counter; lock is applied when the configured threshold is exceeded
behavior signInWithPassword = account AND password -> UserSession OR AuthenticationFailure OR AccountLockedError

// NewUserRequired: oidcSub not yet linked to any User; caller must redirect to registration
behavior signInWithOidc = OidcCallback AND oidcState -> UserSession OR NewUserRequired OR AuthenticationFailure

behavior verifyOtp = Token AND otpCode -> UserSession OR OtpFailure

// Deletes UserSession from DB and removes Token from Redis
behavior signOut = Token -> void

// --- Account management ---
// If InvitationCode given: validate, add user to GroupInvitation groups, mark invitation used
behavior signUp = RegistrationRequest AND InvitationCode? -> User OR EmailAlreadyExistsError OR InvalidInvitationError

behavior changePassword = User AND currentPassword AND newPassword -> PasswordCredential OR PasswordPolicyViolation OR AuthenticationFailure

// Looks up user by identity UserProfileField matching email, then sends a reset code
behavior resetPasswordChallenge = email -> PasswordResetChallenge OR UserNotFoundError

behavior resetPassword = code AND newPassword -> PasswordCredential OR InvalidCodeError OR ExpiredCodeError

behavior updateProfile = User AND profileUpdates -> User OR ValidationError

behavior verifyProfile = User AND UserProfileField AND verificationCode -> UserProfileValue OR InvalidCodeError OR ExpiredCodeError

// --- Invitation ---
behavior createInvitation = email AND List<Group> -> Invitation OR EmailAlreadyExistsError

behavior acceptInvitation = InvitationCode AND RegistrationRequest -> User AND List<GroupInvitation> OR InvalidInvitationError

// --- Lock ---
behavior lockAccount = User AND lockLevel -> UserLock

behavior unlockAccount = User -> void

// --- Authorization (performed by bouncr-proxy, not api-server) ---
// Iterates cached Realm list, matches pathPattern against requestPath
behavior resolveRealm = requestPath -> Realm OR NoRealmError

// 1. Look up Token in Redis (msgpack payload)
// 2. Find User, resolve Assignments for the matched Realm
// 3. Collect Permission names from Roles
// 4. Sign HS256 JWT with permissions claim
// 5. Set x-bouncr-credential header via Envoy ext_proc HeaderMutation
behavior authorizeRequest = Token AND Realm -> JWT OR AuthorizationFailure

// Envoy routes request to Application.passTo (identified by x-bouncr-cluster header)
// Path is rewritten from virtualPath to Application.passTo by ext_proc before routing
behavior routeRequest = JWT AND Application -> upstreamResponse
```
