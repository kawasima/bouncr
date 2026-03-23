import { BeforeAll, AfterAll, Before, After } from '@cucumber/cucumber';
import { chromium, request as playwrightRequest } from '@playwright/test';
import type { Browser, APIRequestContext } from '@playwright/test';
import { BouncrWorld } from './world';
import {
  ADMIN_ACCOUNT,
  ADMIN_PASSWORD,
  TEST_USERS,
  E2E_ROLES,
  E2E_GROUPS,
} from './config';
import {
  signInViaApi,
  createUser,
  createPasswordCredential,
  createGroup,
  createRole,
  addUserToGroup,
  createAssignment,
  setRolePermissions,
  getPermissions,
  getApplications,
  getRealms,
  deleteUser,
  deleteGroup,
  deleteRole,
  getGroup,
  getRole,
} from './auth.helper';

let browser: Browser;
let globalAdminToken: string;
let globalAdminPassword: string;
let globalRequest: APIRequestContext;
// Pre-cached tokens for test users (avoid repeated signInViaApi calls)
const cachedTokens: Record<string, string> = {};

// All known admin-level permissions (prefixed with any_ or bare resource names)
const ADMIN_PERMISSION_PREFIXES = [
  'any_user:', 'user:', 'any_group:', 'group:', 'any_application:', 'application:',
  'any_realm:', 'realm:', 'any_role:', 'role:', 'any_permission:', 'permission:',
  'oidc_application:', 'oidc_provider:', 'invitation:', 'my:',
];

const READONLY_SUFFIXES = [':read'];
const MY_ONLY_NAMES = ['my:read', 'my:update'];
const GROUP_ONLY_NAMES = ['group:read', 'group:create'];

BeforeAll({ timeout: 120_000 }, async function () {
  const headed = process.env.HEADED === '1';
  browser = await chromium.launch({ headless: !headed });
  globalRequest = await playwrightRequest.newContext();

  // 1. Sign in as admin (may change password if initial)
  const adminSignIn = await signInViaApi(globalRequest, ADMIN_ACCOUNT, ADMIN_PASSWORD);
  globalAdminToken = adminSignIn.token;
  globalAdminPassword = adminSignIn.actualPassword;

  // 2. Get all permissions
  const allPermissions = await getPermissions(globalRequest, globalAdminToken);

  // 3. Find the BOUNCR realm
  const apps = await getApplications(globalRequest, globalAdminToken);
  let bouncr_realm: { id: number; name: string } | undefined;
  for (const app of apps) {
    try {
      const realms = await getRealms(globalRequest, globalAdminToken, app.name);
      const bouncrRealm = realms.find((r) => r.name === 'BOUNCR');
      if (bouncrRealm) {
        bouncr_realm = { id: bouncrRealm.id, name: bouncrRealm.name };
        break;
      }
    } catch {
      // continue
    }
  }
  if (!bouncr_realm) {
    throw new Error('Could not find BOUNCR realm in seed data');
  }

  // 4. Create roles with specific permission sets
  const adminPerms = allPermissions.filter((p) =>
    ADMIN_PERMISSION_PREFIXES.some((prefix) => p.name.startsWith(prefix)),
  );
  const readOnlyPerms = allPermissions.filter((p) =>
    READONLY_SUFFIXES.some((suffix) => p.name.endsWith(suffix)),
  );
  const myOnlyPerms = allPermissions.filter((p) => MY_ONLY_NAMES.includes(p.name));
  const groupOnlyPerms = allPermissions.filter((p) => GROUP_ONLY_NAMES.includes(p.name));

  const roleConfigs = [
    { name: E2E_ROLES.admin, description: 'E2E Admin Role', perms: adminPerms },
    { name: E2E_ROLES.readonly, description: 'E2E ReadOnly Role', perms: readOnlyPerms },
    { name: E2E_ROLES.myOnly, description: 'E2E My-Only Role', perms: myOnlyPerms },
    { name: E2E_ROLES.groupOnly, description: 'E2E Group-Only Role', perms: groupOnlyPerms },
  ];

  for (const rc of roleConfigs) {
    try {
      await createRole(globalRequest, globalAdminToken, rc.name, rc.description);
    } catch {
      // Role may already exist if tests were re-run without DB reset
    }
    const roleObj = await getRole(globalRequest, globalAdminToken, rc.name);
    await setRolePermissions(
      globalRequest,
      globalAdminToken,
      rc.name,
      rc.perms.map((p) => p.name),
    );

    // 5. Create corresponding group
    const groupName = Object.values(E2E_GROUPS)[roleConfigs.indexOf(rc)];
    const groupDescription = `E2E group for ${rc.name}`;
    try {
      await createGroup(globalRequest, globalAdminToken, groupName, groupDescription);
    } catch {
      // Group may already exist
    }
    const groupObj = await getGroup(globalRequest, globalAdminToken, groupName);

    // 6. Create assignment linking group to role for BOUNCR realm
    try {
      await createAssignment(
        globalRequest,
        globalAdminToken,
        { id: groupObj.id, name: groupObj.name },
        { id: roleObj.id, name: roleObj.name },
        bouncr_realm,
      );
    } catch {
      // Assignment may already exist
    }
  }

  // 7. Create test users and add them to groups
  const userGroupMap: Record<string, string> = {
    e2e_admin: E2E_GROUPS.admin,
    e2e_readonly: E2E_GROUPS.readonly,
    e2e_no_admin: E2E_GROUPS.myOnly,
    e2e_group_only: E2E_GROUPS.groupOnly,
  };

  for (const [key, userInfo] of Object.entries(TEST_USERS)) {
    try {
      await createUser(globalRequest, globalAdminToken, userInfo.account, userInfo.name, userInfo.email);
    } catch {
      // User may already exist
    }
    try {
      await createPasswordCredential(globalRequest, globalAdminToken, userInfo.account, userInfo.password);
    } catch {
      // Credential may already exist
    }
    const groupName = userGroupMap[key];
    if (groupName) {
      try {
        await addUserToGroup(globalRequest, globalAdminToken, groupName, userInfo.account);
      } catch {
        // May already be a member
      }
    }
  }

  // 8. Pre-cache tokens for all test users
  cachedTokens['admin'] = globalAdminToken;
  for (const [key, userInfo] of Object.entries(TEST_USERS)) {
    try {
      const result = await signInViaApi(globalRequest, userInfo.account, userInfo.password);
      cachedTokens[key] = result.token;
    } catch {
      // Token will be obtained on demand if needed
    }
  }
});

Before({ timeout: 30_000 }, async function (this: BouncrWorld) {
  this.browser = browser;
  this.context = await browser.newContext();
  this.page = await this.context.newPage();
  this.request = await playwrightRequest.newContext();
  this.adminToken = globalAdminToken;
  this.adminPassword = globalAdminPassword;
  this.cachedTokens = cachedTokens;
  this.testData = new Map();
});

After(async function (this: BouncrWorld) {
  if (this.page) {
    await this.page.close();
  }
  if (this.context) {
    await this.context.close();
  }
  if (this.request) {
    await this.request.dispose();
  }
});

AfterAll(async function () {
  // Cleanup: delete test users, groups, roles
  if (globalRequest && globalAdminToken) {
    for (const userInfo of Object.values(TEST_USERS)) {
      await deleteUser(globalRequest, globalAdminToken, userInfo.account);
    }
    for (const groupName of Object.values(E2E_GROUPS)) {
      await deleteGroup(globalRequest, globalAdminToken, groupName);
    }
    for (const roleName of Object.values(E2E_ROLES)) {
      await deleteRole(globalRequest, globalAdminToken, roleName);
    }
    await globalRequest.dispose();
  }
  if (browser) {
    await browser.close();
  }
});
