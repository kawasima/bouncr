import { apiRequest } from './client';
import type { RegistrationOptions, AuthenticationOptions } from '@/lib/webauthn';
import type {
  User,
  UserSession,
  WebAuthnCredentialInfo,
  WebAuthnSignInResponse,
  SignInRequest,
  SignUpRequest,
  PasswordCredentialRequest,
  PasswordCredentialCreateRequest,
  PasswordResetChallengeRequest,
  PasswordResetRequest,
  InitialPassword,
  OtpKey,
  Group,
  Role,
  Permission,
  Application,
  Realm,
  OidcProvider,
  OidcProviderCreateRequest,
  OidcProviderUpdateRequest,
  OidcApplication,
  OidcApplicationCreateRequest,
  OidcApplicationUpdateRequest,
  Invitation,
  InvitationCreateRequest,
  Assignment,
  AssignmentRequest,
  UserAction,
  NameDescriptionRequest,
  ApplicationCreateRequest,
  ApplicationUpdateRequest,
  RealmCreateRequest,
  RealmUpdateRequest,
  UserSearchParams,
  ActionSearchParams,
  PaginationParams,
} from './types';

// === Auth ===

export function signIn(data: SignInRequest) {
  return apiRequest<UserSession>('/sign_in', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function signUp(data: SignUpRequest) {
  return apiRequest<InitialPassword>('/sign_up', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function signOut(sessionToken: string, token: string) {
  return apiRequest<void>(`/session/${sessionToken}`, {
    method: 'DELETE',
    token,
  });
}

// === Users ===

export function getUsers(params: UserSearchParams, token: string) {
  return apiRequest<User[]>('/users', { token, params: { ...params } });
}

export function getUser(account: string, token: string, embed?: string) {
  return apiRequest<User>(`/user/${encodeURIComponent(account)}`, {
    token,
    params: embed ? { embed } : undefined,
  });
}

export function createUser(data: Record<string, unknown>, token: string) {
  return apiRequest<User>('/users', {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  });
}

export function updateUser(account: string, data: Record<string, unknown>, token: string) {
  return apiRequest<User>(`/user/${encodeURIComponent(account)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
    token,
  });
}

export function deleteUser(account: string, token: string) {
  return apiRequest<void>(`/user/${encodeURIComponent(account)}`, {
    method: 'DELETE',
    token,
  });
}

// === Groups ===

export function getGroups(params: PaginationParams & { q?: string }, token: string) {
  return apiRequest<Group[]>('/groups', { token, params: { ...params } });
}

export function getGroup(name: string, token: string) {
  return apiRequest<Group>(`/group/${encodeURIComponent(name)}`, { token });
}

export function createGroup(data: NameDescriptionRequest, token: string) {
  return apiRequest<Group>('/groups', {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  });
}

export function updateGroup(name: string, data: NameDescriptionRequest, token: string) {
  return apiRequest<Group>(`/group/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
    token,
  });
}

// === Roles ===

export function getRoles(params: PaginationParams & { q?: string }, token: string) {
  return apiRequest<Role[]>('/roles', { token, params: { ...params } });
}

export function getRole(name: string, token: string) {
  return apiRequest<Role>(`/role/${encodeURIComponent(name)}`, { token });
}

export function createRole(data: NameDescriptionRequest, token: string) {
  return apiRequest<Role>('/roles', {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  });
}

export function updateRole(name: string, data: NameDescriptionRequest, token: string) {
  return apiRequest<Role>(`/role/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
    token,
  });
}

export function updateRolePermissions(roleName: string, permissions: Permission[], token: string) {
  return apiRequest<void>(`/role/${encodeURIComponent(roleName)}/permissions`, {
    method: 'PUT',
    body: JSON.stringify(permissions),
    token,
  });
}

// === Permissions ===

export function getPermissions(params: PaginationParams & { q?: string }, token: string) {
  return apiRequest<Permission[]>('/permissions', { token, params: { ...params } });
}

export function getPermission(name: string, token: string) {
  return apiRequest<Permission>(`/permission/${encodeURIComponent(name)}`, { token });
}

export function createPermission(data: NameDescriptionRequest, token: string) {
  return apiRequest<Permission>('/permissions', {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  });
}

export function updatePermission(name: string, data: NameDescriptionRequest, token: string) {
  return apiRequest<Permission>(`/permission/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
    token,
  });
}

// === Applications ===

export function getApplications(params: PaginationParams & { q?: string }, token: string) {
  return apiRequest<Application[]>('/applications', { token, params: { ...params } });
}

export function getApplication(name: string, token: string) {
  return apiRequest<Application>(`/application/${encodeURIComponent(name)}`, { token });
}

export function createApplication(data: ApplicationCreateRequest, token: string) {
  return apiRequest<Application>('/applications', {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  });
}

export function updateApplication(name: string, data: ApplicationUpdateRequest, token: string) {
  return apiRequest<Application>(`/application/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
    token,
  });
}

// === OTP ===

export function getOtpKey(token: string) {
  return apiRequest<OtpKey>('/otp_key', { token });
}

export function createOtpKey(token: string) {
  return apiRequest<OtpKey>('/otp_key', { method: 'PUT', token });
}

export function deleteOtpKey(token: string) {
  return apiRequest<void>('/otp_key', { method: 'DELETE', token });
}

// === WebAuthn ===

export function getWebAuthnRegisterOptions(token: string) {
  return apiRequest<RegistrationOptions>('/my/webauthn/register/options', {
    method: 'POST',
    token,
  });
}

export function registerWebAuthn(registrationResponseJSON: string, credentialName: string | null, token: string) {
  return apiRequest<WebAuthnCredentialInfo>('/my/webauthn/register', {
    method: 'POST',
    body: JSON.stringify({
      registration_response_json: registrationResponseJSON,
      credential_name: credentialName,
    }),
    token,
  });
}

export function getWebAuthnCredentials(token: string) {
  return apiRequest<WebAuthnCredentialInfo[]>('/my/webauthn/credentials', { token });
}

export function deleteWebAuthnCredential(id: number, token: string) {
  return apiRequest<void>('/my/webauthn/credentials', {
    method: 'DELETE',
    token,
    params: { id },
  });
}

export function getWebAuthnSignInOptions(account?: string) {
  return apiRequest<AuthenticationOptions>('/sign_in/webauthn/options', {
    method: 'POST',
    body: JSON.stringify(account ? { account } : {}),
  });
}

export function signInWithWebAuthn(authenticationResponseJSON: string) {
  return apiRequest<WebAuthnSignInResponse>('/sign_in/webauthn', {
    method: 'POST',
    body: JSON.stringify({ authentication_response_json: authenticationResponseJSON }),
  });
}

// === Actions (Audit) ===

export function getActions(params: ActionSearchParams, token: string) {
  return apiRequest<UserAction[]>('/actions', { token, params: { ...params } });
}

// === Password ===

export function updatePassword(data: PasswordCredentialRequest, token?: string) {
  return apiRequest<void>('/password_credential', {
    method: 'PUT',
    body: JSON.stringify(data),
    token,
  });
}

export function requestPasswordReset(data: PasswordResetChallengeRequest) {
  return apiRequest<void>('/password_credential/reset_code', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function resetPassword(data: PasswordResetRequest) {
  return apiRequest<InitialPassword>('/password_credential/reset', {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

// === Password Credential ===

export function createPasswordCredential(data: PasswordCredentialCreateRequest, token: string) {
  return apiRequest<void>('/password_credential', {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  });
}

// === Realms ===

export function getRealms(appName: string, token: string) {
  return apiRequest<Realm[]>(`/application/${encodeURIComponent(appName)}/realms`, { token });
}

export function getRealm(appName: string, realmName: string, token: string) {
  return apiRequest<Realm>(`/application/${encodeURIComponent(appName)}/realm/${encodeURIComponent(realmName)}`, { token });
}

export function createRealm(appName: string, data: RealmCreateRequest, token: string) {
  return apiRequest<Realm>(`/application/${encodeURIComponent(appName)}/realms`, {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  });
}

export function updateRealm(appName: string, realmName: string, data: RealmUpdateRequest, token: string) {
  return apiRequest<Realm>(`/application/${encodeURIComponent(appName)}/realm/${encodeURIComponent(realmName)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
    token,
  });
}

export function deleteRealm(appName: string, realmName: string, token: string) {
  return apiRequest<void>(`/application/${encodeURIComponent(appName)}/realm/${encodeURIComponent(realmName)}`, {
    method: 'DELETE',
    token,
  });
}

// === OIDC Providers ===

export function getOidcProviders(params: PaginationParams & { q?: string }, token: string) {
  return apiRequest<OidcProvider[]>('/oidc_providers', { token, params: { ...params } });
}

export function getOidcProvider(name: string, token: string) {
  return apiRequest<OidcProvider>(`/oidc_provider/${encodeURIComponent(name)}`, { token });
}

export function createOidcProvider(data: OidcProviderCreateRequest, token: string) {
  return apiRequest<OidcProvider>('/oidc_providers', {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  });
}

export function updateOidcProvider(name: string, data: OidcProviderUpdateRequest, token: string) {
  return apiRequest<OidcProvider>(`/oidc_provider/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
    token,
  });
}

export function deleteOidcProvider(name: string, token: string) {
  return apiRequest<void>(`/oidc_provider/${encodeURIComponent(name)}`, {
    method: 'DELETE',
    token,
  });
}

// === OIDC Applications ===

export function getOidcApplications(params: PaginationParams & { q?: string }, token: string) {
  return apiRequest<OidcApplication[]>('/oidc_applications', { token, params: { ...params } });
}

export function getOidcApplication(name: string, token: string) {
  return apiRequest<OidcApplication>(`/oidc_application/${encodeURIComponent(name)}`, { token });
}

export function createOidcApplication(data: OidcApplicationCreateRequest, token: string) {
  return apiRequest<OidcApplication>('/oidc_applications', {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  });
}

export function updateOidcApplication(name: string, data: OidcApplicationUpdateRequest, token: string) {
  return apiRequest<OidcApplication>(`/oidc_application/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
    token,
  });
}

export function deleteOidcApplication(name: string, token: string) {
  return apiRequest<void>(`/oidc_application/${encodeURIComponent(name)}`, {
    method: 'DELETE',
    token,
  });
}

// === Invitations ===

export function getInvitations(params: PaginationParams, token: string) {
  return apiRequest<Invitation[]>('/invitations', { token, params: { ...params } });
}

export function getInvitation(code: string, token: string) {
  return apiRequest<Invitation>(`/invitation/${encodeURIComponent(code)}`, { token });
}

export function createInvitation(data: InvitationCreateRequest, token: string) {
  return apiRequest<Invitation>('/invitations', {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  });
}

// === Group Users ===

export function getGroupUsers(groupName: string, token: string) {
  return apiRequest<User[]>(`/group/${encodeURIComponent(groupName)}/users`, { token });
}

export function addGroupUsers(groupName: string, accounts: string[], token: string) {
  return apiRequest<User[]>(`/group/${encodeURIComponent(groupName)}/users`, {
    method: 'POST',
    body: JSON.stringify(accounts),
    token,
  });
}

export function removeGroupUsers(groupName: string, accounts: string[], token: string) {
  return apiRequest<void>(`/group/${encodeURIComponent(groupName)}/users`, {
    method: 'DELETE',
    body: JSON.stringify(accounts),
    token,
  });
}

// === Assignments ===

export function getAssignment(params: { groupId: number; roleId: number; realmId: number }, token: string) {
  return apiRequest<Assignment>('/assignment', { token, params: { ...params } });
}

export function createAssignments(assignments: AssignmentRequest[], token: string) {
  return apiRequest<void>('/assignments', {
    method: 'POST',
    body: JSON.stringify(assignments),
    token,
  });
}

export function deleteAssignments(assignments: AssignmentRequest[], token: string) {
  return apiRequest<void>('/assignments', {
    method: 'DELETE',
    body: JSON.stringify(assignments),
    token,
  });
}

// === Email Verification ===

export function verifyEmail(code: string) {
  return apiRequest<void>('/user_profile_verification', {
    method: 'DELETE',
    params: { code },
  });
}
