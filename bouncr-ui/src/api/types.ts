// === Domain Entities ===

export interface User {
  id: number;
  account: string;
  email?: string;
  name?: string;
  permissions?: string[];
  unverified_profiles?: string[];
  groups?: Group[];
  oidc_providers?: string[];
  [key: string]: unknown;
}

export interface Application {
  id: number;
  name: string;
  description: string;
  pass_to: string;
  virtual_path: string;
  top_page: string;
  realms?: Realm[];
}

export interface Realm {
  id: number;
  name: string;
  description: string;
  url: string;
  assignments?: Assignment[];
}

export interface Group {
  id: number;
  name: string;
  description: string;
  writeProtected?: boolean;
  users?: User[];
}

export interface Role {
  id: number;
  name: string;
  description: string;
  permissions?: Permission[];
}

export interface Permission {
  id: number;
  name: string;
  description: string;
}

export interface UserAction {
  id: number;
  action_type: string;
  actor: string;
  actor_ip: string;
  options?: string;
  created_at: string;
}

export interface OidcProvider {
  id: number;
  name: string;
  clientId: string;
  clientSecret?: string;
  scope: string;
  responseType: string;
  authorizationEndpoint: string;
  tokenEndpoint: string;
  tokenEndpointAuthMethod: string;
  redirectUri?: string;
  jwksUri?: string;
  issuer?: string;
  pkceEnabled?: boolean;
}

export interface OidcApplication {
  id: number;
  name: string;
  client_id?: string;
  client_secret?: string;
  home_url: string;
  callback_url: string;
  description: string;
  backchannel_logout_uri?: string;
  frontchannel_logout_uri?: string;
  permissions?: Permission[];
}

export interface SignOutResponse {
  frontchannel_logout_urls: string[];
  backchannel_logout: {
    attempted: number;
    succeeded: number;
    failed: number;
  };
}

export interface Invitation {
  id: number;
  code: string;
  email: string;
  invitedAt: string;
  groupInvitations?: GroupInvitation[];
  oidcInvitations?: OidcInvitation[];
}

export interface GroupInvitation {
  id: number;
  group: Group;
}

export interface OidcInvitation {
  id: number;
  oidcProvider: OidcProvider;
  oidcPayload: string;
}

export interface Assignment {
  group: { id: number; name: string };
  role: { id: number; name: string };
  realm: { id: number; name: string };
}

export interface UserLock {
  lockLevel: string;
  lockedAt: string;
}

export interface UserProfileField {
  id: number;
  name: string;
  jsonName: string;
  isRequired: boolean;
  isIdentity: boolean;
  regularExpression?: string;
  maxLength?: number;
  minLength?: number;
  needsVerification?: boolean;
  position?: number;
}

export interface OtpKey {
  key?: string | null;
}

export interface WebAuthnCredentialInfo {
  id: number;
  credential_name: string;
  transports: string;
  discoverable: boolean;
}

export interface InitialPassword {
  password: string;
}

export interface UserSession {
  token: string;
  remote_address: string;
  user_agent: string;
  created_at: string;
}

export interface WebAuthnSignInResponse {
  token: string;
  account: string;
}

// === API Error ===

export interface Problem {
  status: number;
  detail?: string;
  type?: string;
  violations?: Violation[];
}

export interface Violation {
  field: string;
  message: string;
}

// === Request Types ===

export interface SignInRequest {
  account: string;
  password: string;
  one_time_password?: string;
}

export interface SignUpRequest {
  account: string;
  code?: string;
  enable_password_credential?: boolean;
  [key: string]: unknown;
}

export interface PasswordCredentialRequest {
  account: string;
  old_password: string;
  new_password: string;
}

export interface PasswordResetChallengeRequest {
  account: string;
}

export interface PasswordResetRequest {
  code: string;
}

export interface UserCreateRequest {
  account: string;
  [key: string]: unknown;
}

export interface UserUpdateRequest {
  [key: string]: unknown;
}

export interface NameDescriptionRequest {
  name: string;
  description: string;
}

export interface ApplicationCreateRequest {
  name: string;
  description: string;
  pass_to: string;
  virtual_path: string;
  top_page: string;
}

export interface ApplicationUpdateRequest {
  description?: string;
  pass_to?: string;
  virtual_path?: string;
  top_page?: string;
}

export interface RealmCreateRequest {
  name: string;
  description: string;
  url: string;
}

export interface RealmUpdateRequest {
  name: string;
  description: string;
}

export interface OidcProviderCreateRequest {
  name: string;
  clientId: string;
  clientSecret: string;
  scope: string;
  responseType: string;
  authorizationEndpoint: string;
  tokenEndpoint: string;
  tokenEndpointAuthMethod: string;
  redirectUri?: string;
  jwksUri?: string;
  issuer?: string;
  pkceEnabled?: boolean;
}

export interface OidcProviderUpdateRequest extends OidcProviderCreateRequest {}

export interface OidcApplicationCreateRequest {
  name: string;
  home_url: string;
  callback_url: string;
  description: string;
  backchannel_logout_uri?: string;
  frontchannel_logout_uri?: string;
  permissions?: string[];
}

export interface OidcApplicationUpdateRequest extends OidcApplicationCreateRequest {}

export interface InvitationCreateRequest {
  email: string;
  groups?: { id: number }[];
}

export interface AssignmentRequest {
  group: { id: number; name?: string };
  role: { id: number; name?: string };
  realm: { id: number; name?: string };
}

export interface PasswordCredentialCreateRequest {
  account: string;
  password: string;
  initial?: boolean;
}

// === Search Params ===

export interface PaginationParams {
  limit?: number;
  offset?: number;
}

export interface UserSearchParams extends PaginationParams {
  q?: string;
  embed?: string;
}

export interface ActionSearchParams extends PaginationParams {
  actor?: string;
}
