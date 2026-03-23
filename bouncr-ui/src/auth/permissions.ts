/**
 * Permission requirements for each admin resource.
 * Each entry lists alternative permissions (OR logic).
 */
export const RESOURCE_PERMISSIONS = {
  user:            { read: ['any_user:read', 'user:read'],                   create: ['any_user:create', 'user:create'],                   delete: ['any_user:delete', 'user:delete'] },
  group:           { read: ['any_group:read', 'group:read'],                 create: ['any_group:create', 'group:create'],                 delete: ['any_group:delete', 'group:delete'] },
  application:     { read: ['any_application:read', 'application:read'],     create: ['any_application:create', 'application:create'] },
  role:            { read: ['any_role:read', 'role:read'],                   create: ['any_role:create', 'role:create'] },
  permission:      { read: ['any_permission:read', 'permission:read'],       create: ['any_permission:create', 'permission:create'] },
  oidcApplication: { read: ['oidc_application:read'],                        create: ['oidc_application:create'],                          delete: ['oidc_application:delete'], update: ['oidc_application:update'] },
  oidcProvider:    { read: ['oidc_provider:read'],                           create: ['oidc_provider:create'] },
  invitation:      { create: ['invitation:create'] },
} as const;
