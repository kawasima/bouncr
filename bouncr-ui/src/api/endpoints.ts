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
  SignOutResponse,
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

export function signOut() {
  return apiRequest<SignOutResponse>('/session/me', {
    method: 'DELETE',
  });
}

// === Users ===

export function getUsers(params: UserSearchParams) {
  return apiRequest<User[]>('/users', { params: { ...params } });
}

export function getUser(account: string, embed?: string) {
  return apiRequest<User>(`/user/${encodeURIComponent(account)}`, {
    params: embed ? { embed } : undefined,
  });
}

export function createUser(data: Record<string, unknown>) {
  return apiRequest<User>('/users', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateUser(account: string, data: Record<string, unknown>) {
  return apiRequest<User>(`/user/${encodeURIComponent(account)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function deleteUser(account: string) {
  return apiRequest<void>(`/user/${encodeURIComponent(account)}`, {
    method: 'DELETE',
  });
}

// === Groups ===

export function getGroups(params: PaginationParams & { q?: string }) {
  return apiRequest<Group[]>('/groups', { params: { ...params } });
}

export function getGroup(name: string) {
  return apiRequest<Group>(`/group/${encodeURIComponent(name)}`, {});
}

export function createGroup(data: NameDescriptionRequest) {
  return apiRequest<Group>('/groups', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateGroup(name: string, data: NameDescriptionRequest) {
  return apiRequest<Group>(`/group/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

// === Roles ===

export function getRoles(params: PaginationParams & { q?: string }) {
  return apiRequest<Role[]>('/roles', { params: { ...params } });
}

export function getRole(name: string) {
  return apiRequest<Role>(`/role/${encodeURIComponent(name)}`, {});
}

export function createRole(data: NameDescriptionRequest) {
  return apiRequest<Role>('/roles', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateRole(name: string, data: NameDescriptionRequest) {
  return apiRequest<Role>(`/role/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function updateRolePermissions(roleName: string, permissions: Permission[]) {
  return apiRequest<void>(`/role/${encodeURIComponent(roleName)}/permissions`, {
    method: 'PUT',
    body: JSON.stringify(permissions),
  });
}

// === Permissions ===

export function getPermissions(params: PaginationParams & { q?: string }) {
  return apiRequest<Permission[]>('/permissions', { params: { ...params } });
}

export function getPermission(name: string) {
  return apiRequest<Permission>(`/permission/${encodeURIComponent(name)}`, {});
}

export function createPermission(data: NameDescriptionRequest) {
  return apiRequest<Permission>('/permissions', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updatePermission(name: string, data: NameDescriptionRequest) {
  return apiRequest<Permission>(`/permission/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

// === Applications ===

export function getApplications(params: PaginationParams & { q?: string }) {
  return apiRequest<Application[]>('/applications', { params: { ...params } });
}

export function getApplication(name: string) {
  return apiRequest<Application>(`/application/${encodeURIComponent(name)}`, {});
}

export function createApplication(data: ApplicationCreateRequest) {
  return apiRequest<Application>('/applications', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateApplication(name: string, data: ApplicationUpdateRequest) {
  return apiRequest<Application>(`/application/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

// === OTP ===

export function getOtpKey() {
  return apiRequest<OtpKey>('/otp_key', {});
}

export function createOtpKey() {
  return apiRequest<OtpKey>('/otp_key', { method: 'PUT' });
}

export function deleteOtpKey() {
  return apiRequest<void>('/otp_key', { method: 'DELETE' });
}

// === WebAuthn ===

export function getWebAuthnRegisterOptions() {
  return apiRequest<RegistrationOptions>('/my/webauthn/register/options', {
    method: 'POST',
  });
}

export function registerWebAuthn(registrationResponseJSON: string, credentialName: string | null) {
  return apiRequest<WebAuthnCredentialInfo>('/my/webauthn/register', {
    method: 'POST',
    body: JSON.stringify({
      registration_response_json: registrationResponseJSON,
      credential_name: credentialName,
    }),
  });
}

export function getWebAuthnCredentials() {
  return apiRequest<WebAuthnCredentialInfo[]>('/my/webauthn/credentials', {});
}

export function deleteWebAuthnCredential(id: number) {
  return apiRequest<void>('/my/webauthn/credentials', {
    method: 'DELETE',
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

export function getActions(params: ActionSearchParams) {
  return apiRequest<UserAction[]>('/actions', { params: { ...params } });
}

// === Password ===

export function updatePassword(data: PasswordCredentialRequest) {
  return apiRequest<void>('/password_credential', {
    method: 'PUT',
    body: JSON.stringify(data),
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

export function createPasswordCredential(data: PasswordCredentialCreateRequest) {
  return apiRequest<void>('/password_credential', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

// === Realms ===

export function getRealms(appName: string) {
  return apiRequest<Realm[]>(`/application/${encodeURIComponent(appName)}/realms`, {});
}

export function getRealm(appName: string, realmName: string) {
  return apiRequest<Realm>(`/application/${encodeURIComponent(appName)}/realm/${encodeURIComponent(realmName)}`, {});
}

export function createRealm(appName: string, data: RealmCreateRequest) {
  return apiRequest<Realm>(`/application/${encodeURIComponent(appName)}/realms`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateRealm(appName: string, realmName: string, data: RealmUpdateRequest) {
  return apiRequest<Realm>(`/application/${encodeURIComponent(appName)}/realm/${encodeURIComponent(realmName)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function deleteRealm(appName: string, realmName: string) {
  return apiRequest<void>(`/application/${encodeURIComponent(appName)}/realm/${encodeURIComponent(realmName)}`, {
    method: 'DELETE',
  });
}

// === OIDC Providers ===

export function getOidcProviders(params: PaginationParams & { q?: string }) {
  return apiRequest<OidcProvider[]>('/oidc_providers', { params: { ...params } });
}

export function getOidcProvider(name: string) {
  return apiRequest<OidcProvider>(`/oidc_provider/${encodeURIComponent(name)}`, {});
}

export function createOidcProvider(data: OidcProviderCreateRequest) {
  return apiRequest<OidcProvider>('/oidc_providers', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateOidcProvider(name: string, data: OidcProviderUpdateRequest) {
  return apiRequest<OidcProvider>(`/oidc_provider/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function deleteOidcProvider(name: string) {
  return apiRequest<void>(`/oidc_provider/${encodeURIComponent(name)}`, {
    method: 'DELETE',
  });
}

// === OIDC Applications ===

export function getOidcApplications(params: PaginationParams & { q?: string }) {
  return apiRequest<OidcApplication[]>('/oidc_applications', { params: { ...params } });
}

export function getOidcApplication(name: string) {
  return apiRequest<OidcApplication>(`/oidc_application/${encodeURIComponent(name)}`, {});
}

export function createOidcApplication(data: OidcApplicationCreateRequest) {
  return apiRequest<OidcApplication>('/oidc_applications', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateOidcApplication(name: string, data: OidcApplicationUpdateRequest) {
  return apiRequest<OidcApplication>(`/oidc_application/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function deleteOidcApplication(name: string) {
  return apiRequest<void>(`/oidc_application/${encodeURIComponent(name)}`, {
    method: 'DELETE',
  });
}

// === Invitations ===

export function getInvitations(params: PaginationParams) {
  return apiRequest<Invitation[]>('/invitations', { params: { ...params } });
}

export function getInvitation(code: string) {
  return apiRequest<Invitation>(`/invitation/${encodeURIComponent(code)}`, {});
}

export function createInvitation(data: InvitationCreateRequest) {
  return apiRequest<Invitation>('/invitations', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

// === Group Users ===

export function getGroupUsers(groupName: string) {
  return apiRequest<User[]>(`/group/${encodeURIComponent(groupName)}/users`, {});
}

export function addGroupUsers(groupName: string, accounts: string[]) {
  return apiRequest<User[]>(`/group/${encodeURIComponent(groupName)}/users`, {
    method: 'POST',
    body: JSON.stringify(accounts),
  });
}

export function removeGroupUsers(groupName: string, accounts: string[]) {
  return apiRequest<void>(`/group/${encodeURIComponent(groupName)}/users`, {
    method: 'DELETE',
    body: JSON.stringify(accounts),
  });
}

// === Assignments ===

export function getAssignment(params: { groupId: number; roleId: number; realmId: number }) {
  return apiRequest<Assignment>('/assignment', { params: { ...params } });
}

export function createAssignments(assignments: AssignmentRequest[]) {
  return apiRequest<void>('/assignments', {
    method: 'POST',
    body: JSON.stringify(assignments),
  });
}

export function deleteAssignments(assignments: AssignmentRequest[]) {
  return apiRequest<void>('/assignments', {
    method: 'DELETE',
    body: JSON.stringify(assignments),
  });
}

// === Email Verification ===

export function verifyEmail(code: string) {
  return apiRequest<void>('/user_profile_verification', {
    method: 'DELETE',
    params: { code },
  });
}
