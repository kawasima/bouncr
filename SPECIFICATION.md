# Specification model

```specification
// ===== Primitive values =====
data UserId = Long
data GroupId = Long
data ApplicationId = Long
data RoleId = Long
data PermissionId = Long
data RealmId = Long
data InvitationCode = String  // 8 random alphanumeric characters
data Token = String           // UUID, used as Redis key for session lookup
data JWT = String             // HS256-signed, injected as x-bouncr-credential header

// ===== User =====
data User = UserId AND account AND List<Group> AND List<UserProfileValue> AND UserLock? AND PasswordCredential? AND OtpKey? AND List<OidcUser>
// account: case-insensitive unique string (accountLower stored separately for lookup)
// User has no email column — email is stored as a UserProfileValue

// ===== User profile =====
data UserProfileField = UserProfileFieldId AND name AND jsonName AND isRequired AND isIdentity AND needsVerification AND regularExpression? AND maxLength? AND minLength?
// isIdentity=true: this field is used for user lookup (e.g. email address)
// needsVerification=true: value change requires email verification before it takes effect

data UserProfileValue = UserProfileField AND User AND value
// The actual stored value for a profile field; serialized under jsonName in API responses

data UserProfileVerification = UserProfileValue AND code AND expiresAt
// Pending verification record created when a needsVerification field is changed

// ===== Password credential =====
data PasswordCredential = User AND passwordHash AND salt AND initial AND createdAt
// initial=true: temporary password, user must change on first login
// hashing algorithm is PBKDF2 / bcrypt (not stored in the entity)

// ===== OTP =====
data OtpKey = User AND key
// TOTP shared secret for two-factor authentication

// ===== Lock =====
data UserLock = User AND lockLevel AND lockedAt
// lockLevel: LockLevel enum
// Created after repeated authentication failures; removed by admin unlock

// ===== Session =====
data UserSession = Token AND User AND remoteAddress AND userAgent AND createdAt
// Persisted in DB; Token is also written to Redis (msgpack) with permissions for fast lookup

// ===== Groups and roles =====
data Group = GroupId AND name AND description? AND List<User>
// Membership is stored in the memberships join table (many-to-many with User)

data Role = RoleId AND name AND description? AND List<Permission>
// Permissions are granted to a Role; Roles are assigned to a Group within a Realm

data Permission = PermissionId AND name AND description?

// ===== Access control =====
data Application = ApplicationId AND name AND description? AND virtualPath AND passTo AND topPage? AND List<Realm>
// virtualPath: URL prefix exposed to clients (e.g. /myapp)
// passTo: upstream backend URL for Envoy routing (e.g. http://backend:8080)

data Realm = RealmId AND name AND url AND description? AND Application AND List<Assignment>
// url: URL pattern suffix relative to Application.virtualPath
// pathPattern (transient, not persisted): compiled as ^{virtualPath}($|/{url})

data Assignment = Group AND Role AND Realm
// Grants a Role to a Group scoped to a Realm
// Effective permissions for a request: User -> memberships -> Groups
//   -> Assignments matching resolved Realm -> Roles -> Permissions

// ===== OIDC =====
data OidcProvider = OidcProviderId AND name AND clientId AND clientSecret AND scope AND responseType AND authorizationEndpoint AND tokenEndpoint AND tokenEndpointAuthMethod AND redirectUri AND jwksUri AND issuer AND pkceEnabled

data OidcUser = OidcProvider AND User AND oidcSub
// Links an external OIDC identity (issuer + sub) to a Bouncr User

// ===== Invitation =====
data Invitation = InvitationCode AND email AND invitedAt AND List<GroupInvitation> AND List<OidcInvitation>

data GroupInvitation = Invitation AND Group
// Records which groups the invited user will be added to upon completing registration

data OidcInvitation = Invitation AND OidcProvider
// Records which OIDC provider the invited user must authenticate through

// ===== Password reset =====
data PasswordResetChallenge = User AND code AND expiresAt

// ===== Audit log =====
data UserAction = User AND actionType AND createdAt AND remoteAddress? AND description?
// actionType: Action enum (SIGN_IN, SIGN_OUT, CHANGE_PASSWORD, etc.)

// ===== mTLS certificate =====
data Cert = serial AND commonName AND issuerCommonName AND expiresAt

// ===== Behaviors =====

// --- Authentication ---
behavior signInWithPassword = account AND password -> UserSession OR AuthenticationFailure OR AccountLockedError
// Failure increments a counter; lock is applied when the configured threshold is exceeded

behavior signInWithOidc = OidcCallback AND oidcState -> UserSession OR NewUserRequired OR AuthenticationFailure
// NewUserRequired: oidcSub not yet linked to any User; caller must redirect to registration

behavior verifyOtp = Token AND otpCode -> UserSession OR OtpFailure

behavior signOut = Token -> void
// Deletes UserSession from DB and removes Token from Redis

// --- Account management ---
behavior signUp = RegistrationRequest AND InvitationCode? -> User OR EmailAlreadyExistsError OR InvalidInvitationError
// If InvitationCode given: validate, add user to GroupInvitation groups, mark invitation used

behavior changePassword = User AND currentPassword AND newPassword -> PasswordCredential OR PasswordPolicyViolation OR AuthenticationFailure

behavior resetPasswordChallenge = email -> PasswordResetChallenge OR UserNotFoundError
// Looks up user by identity UserProfileField matching email, then sends a reset code

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
behavior resolveRealm = requestPath -> Realm OR NoRealmError
// Iterates cached Realm list, matches pathPattern against requestPath

behavior authorizeRequest = Token AND Realm -> JWT OR AuthorizationFailure
// 1. Look up Token in Redis (msgpack payload)
// 2. Find User, resolve Assignments for the matched Realm
// 3. Collect Permission names from Roles
// 4. Sign HS256 JWT with permissions claim
// 5. Set x-bouncr-credential header via Envoy ext_proc HeaderMutation

behavior routeRequest = JWT AND Application -> upstreamResponse
// Envoy routes request to Application.passTo (identified by x-bouncr-cluster header)
// Path is rewritten from virtualPath to Application.passTo by ext_proc before routing
```
