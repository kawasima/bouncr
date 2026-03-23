export const BASE_URL = 'http://localhost:3001';
export const API_URL = 'http://localhost:3001/bouncr/api';
export const ADMIN_ACCOUNT = 'admin';
export const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? 'admin';

export const TEST_USERS = {
  e2e_admin: { account: 'e2e_admin', name: 'E2E Admin', email: 'e2e_admin@example.com', password: 'E2eAdm!n123' },
  e2e_readonly: { account: 'e2e_readonly', name: 'E2E ReadOnly', email: 'e2e_readonly@example.com', password: 'E2eRead0nly!' },
  e2e_no_admin: { account: 'e2e_no_admin', name: 'E2E NoAdmin', email: 'e2e_no_admin@example.com', password: 'E2eN0Adm!n1' },
  e2e_group_only: { account: 'e2e_group_only', name: 'E2E GroupOnly', email: 'e2e_group_only@example.com', password: 'E2eGr0up0nly!' },
} as const;

export const E2E_ROLES = {
  admin: 'E2E_ADMIN_ROLE',
  readonly: 'E2E_READONLY_ROLE',
  myOnly: 'E2E_MY_ONLY_ROLE',
  groupOnly: 'E2E_GROUP_ONLY_ROLE',
} as const;

export const E2E_GROUPS = {
  admin: 'E2E_ADMIN_GROUP',
  readonly: 'E2E_READONLY_GROUP',
  myOnly: 'E2E_MY_ONLY_GROUP',
  groupOnly: 'E2E_GROUP_ONLY_GROUP',
} as const;
