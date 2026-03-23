/**
 * Permission requirements for each admin resource.
 * Each entry lists alternative permissions (OR logic).
 */
export const RESOURCE_PERMISSIONS = {
  user:            { read: ['any_user:read', 'user:read'], create: ['any_user:create', 'user:create'], update: ['any_user:update', 'user:update'], delete: ['any_user:delete', 'user:delete'] },
  group:           { read: ['any_group:read', 'group:read'], create: ['any_group:create', 'group:create'], update: ['any_group:update', 'group:update'], delete: ['any_group:delete', 'group:delete'] },
  application:     { read: ['any_application:read', 'application:read'], create: ['any_application:create', 'application:create'], update: ['any_application:update', 'application:update'], delete: ['any_application:delete', 'application:delete'] },
  realm:           { read: ['any_realm:read', 'realm:read'], create: ['any_realm:create', 'realm:create'], update: ['any_realm:update', 'realm:update'], delete: ['any_realm:delete', 'realm:delete'] },
  role:            { read: ['any_role:read', 'role:read'], create: ['any_role:create', 'role:create'], update: ['any_role:update', 'role:update'], delete: ['any_role:delete', 'role:delete'] },
  permission:      { read: ['any_permission:read', 'permission:read'], create: ['any_permission:create', 'permission:create'], update: ['any_permission:update', 'permission:update'], delete: ['any_permission:delete', 'permission:delete'] },
  oidcApplication: { read: ['oidc_application:read'], create: ['oidc_application:create'], update: ['oidc_application:update'], delete: ['oidc_application:delete'] },
  oidcProvider:    { read: ['oidc_provider:read'], create: ['oidc_provider:create'], update: ['oidc_provider:update'] },
  invitation:      { create: ['invitation:create'] },
} as const;
