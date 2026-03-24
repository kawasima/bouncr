import type { Page, APIRequestContext } from '@playwright/test';
import { API_URL, BASE_URL } from './config';

function headers(token: string) {
  return {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Cookie': `BOUNCR_TOKEN=${token}`,
  };
}

/**
 * Sign in via the UI form.
 */
export async function signIn(page: Page, account: string, password: string): Promise<void> {
  await page.goto(`${BASE_URL}/sign_in`);
  await page.locator('#account').fill(account);
  await page.locator('#password').fill(password);
  await page.getByRole('button', { name: /enter/i }).click();
  await page.waitForURL(`${BASE_URL}/`);
}

export interface SignInResult {
  token: string;
  /** The password that was actually used (may differ from input if initial password was changed). */
  actualPassword: string;
}

/**
 * Sign in via API, returns the session token and actual password.
 * Handles initial password (PASSWORD_MUST_BE_CHANGED) by changing password first.
 */
export async function signInViaApi(request: APIRequestContext, account: string, password: string): Promise<SignInResult> {
  const response = await request.post(`${API_URL}/sign_in`, {
    data: { account, password },
    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
  });

  if (response.status() === 401) {
    const body = await response.text();
    if (body.includes('PASSWORD_MUST_BE_CHANGED')) {
      // First login: change initial password
      const newPassword = password + '_e2e';
      await changePassword(request, account, password, newPassword);
      return signInViaApi(request, account, newPassword);
    }
    // If the original password failed, try the _e2e variant (already changed in a previous run)
    if (!password.endsWith('_e2e')) {
      try {
        return await signInViaApi(request, account, password + '_e2e');
      } catch {
        // Both passwords failed
      }
    }
    throw new Error(`Sign-in failed for ${account}: ${response.status()} ${body}`);
  }

  if (!response.ok()) {
    throw new Error(`Sign-in failed for ${account}: ${response.status()} ${await response.text()}`);
  }
  const body = await response.json();
  return { token: body.token, actualPassword: password };
}

/**
 * Change password via API (unauthenticated, uses old password).
 */
async function changePassword(
  request: APIRequestContext,
  account: string,
  oldPassword: string,
  newPassword: string,
): Promise<void> {
  const response = await request.put(`${API_URL}/password_credential`, {
    data: { account, old_password: oldPassword, new_password: newPassword },
    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
  });
  if (!response.ok()) {
    throw new Error(`Password change failed for ${account}: ${response.status()} ${await response.text()}`);
  }
}

/**
 * Create a user via API.
 */
export async function createUser(
  request: APIRequestContext,
  token: string,
  account: string,
  name: string,
  email: string,
): Promise<Record<string, unknown>> {
  const response = await request.post(`${API_URL}/users`, {
    data: { account, name, email },
    headers: headers(token),
  });
  if (!response.ok()) {
    const text = await response.text();
    throw new Error(`Create user ${account} failed: ${response.status()} ${text}`);
  }
  const text = await response.text();
  return text ? JSON.parse(text) : {};
}

/**
 * Create a password credential for a user.
 */
export async function createPasswordCredential(
  request: APIRequestContext,
  token: string,
  account: string,
  password: string,
): Promise<void> {
  const response = await request.post(`${API_URL}/password_credential`, {
    data: { account, password, initial: true },
    headers: headers(token),
  });
  if (!response.ok()) {
    const text = await response.text();
    throw new Error(`Create password for ${account} failed: ${response.status()} ${text}`);
  }
}

/**
 * Create a group via API.
 */
export async function createGroup(
  request: APIRequestContext,
  token: string,
  name: string,
  description: string,
): Promise<Record<string, unknown>> {
  const response = await request.post(`${API_URL}/groups`, {
    data: { name, description },
    headers: headers(token),
  });
  if (!response.ok()) {
    const text = await response.text();
    throw new Error(`Create group ${name} failed: ${response.status()} ${text}`);
  }
  return response.json();
}

/**
 * Create a role via API.
 */
export async function createRole(
  request: APIRequestContext,
  token: string,
  name: string,
  description: string,
): Promise<Record<string, unknown>> {
  const response = await request.post(`${API_URL}/roles`, {
    data: { name, description },
    headers: headers(token),
  });
  if (!response.ok()) {
    const text = await response.text();
    throw new Error(`Create role ${name} failed: ${response.status()} ${text}`);
  }
  return response.json();
}

/**
 * Add a user to a group.
 */
export async function addUserToGroup(
  request: APIRequestContext,
  token: string,
  groupName: string,
  account: string,
): Promise<void> {
  const response = await request.post(`${API_URL}/group/${encodeURIComponent(groupName)}/users`, {
    data: [account],
    headers: headers(token),
  });
  if (!response.ok()) {
    const text = await response.text();
    throw new Error(`Add user ${account} to group ${groupName} failed: ${response.status()} ${text}`);
  }
}

/**
 * Create an assignment (group-role-realm binding).
 */
export async function createAssignment(
  request: APIRequestContext,
  token: string,
  group: { id: number; name: string },
  role: { id: number; name: string },
  realm: { id: number; name: string },
): Promise<void> {
  const response = await request.post(`${API_URL}/assignments`, {
    data: [{ group, role, realm }],
    headers: headers(token),
  });
  if (!response.ok()) {
    const text = await response.text();
    throw new Error(`Create assignment failed: ${response.status()} ${text}`);
  }
}

/**
 * Set permissions on a role (POST adds permission names).
 */
export async function setRolePermissions(
  request: APIRequestContext,
  token: string,
  roleName: string,
  permissionNames: string[],
): Promise<void> {
  const response = await request.post(`${API_URL}/role/${encodeURIComponent(roleName)}/permissions`, {
    data: permissionNames,
    headers: headers(token),
  });
  if (!response.ok()) {
    const text = await response.text();
    throw new Error(`Set permissions on role ${roleName} failed: ${response.status()} ${text}`);
  }
}

/**
 * Get all permissions.
 */
export async function getPermissions(
  request: APIRequestContext,
  token: string,
): Promise<Array<{ id: number; name: string; description: string }>> {
  const response = await request.get(`${API_URL}/permissions?limit=1000`, {
    headers: headers(token),
  });
  if (!response.ok()) {
    throw new Error(`Get permissions failed: ${response.status()}`);
  }
  return response.json();
}

/**
 * Get all applications.
 */
export async function getApplications(
  request: APIRequestContext,
  token: string,
): Promise<Array<{ id: number; name: string; description: string }>> {
  const response = await request.get(`${API_URL}/applications?limit=1000`, {
    headers: headers(token),
  });
  if (!response.ok()) {
    throw new Error(`Get applications failed: ${response.status()}`);
  }
  return response.json();
}

/**
 * Get realms for an application.
 */
export async function getRealms(
  request: APIRequestContext,
  token: string,
  appName: string,
): Promise<Array<{ id: number; name: string; description: string }>> {
  const response = await request.get(`${API_URL}/application/${encodeURIComponent(appName)}/realms`, {
    headers: headers(token),
  });
  if (!response.ok()) {
    throw new Error(`Get realms for ${appName} failed: ${response.status()}`);
  }
  return response.json();
}

/**
 * Delete a user via API (cleanup).
 */
export async function deleteUser(
  request: APIRequestContext,
  token: string,
  account: string,
): Promise<void> {
  await request.delete(`${API_URL}/user/${encodeURIComponent(account)}`, {
    headers: headers(token),
  });
  // Ignore errors during cleanup
}

/**
 * Delete a group via API (cleanup).
 */
export async function deleteGroup(
  request: APIRequestContext,
  token: string,
  name: string,
): Promise<void> {
  await request.delete(`${API_URL}/group/${encodeURIComponent(name)}`, {
    headers: headers(token),
  });
}

/**
 * Delete a role via API (cleanup).
 */
export async function deleteRole(
  request: APIRequestContext,
  token: string,
  name: string,
): Promise<void> {
  // First get permissions to remove them
  const permsResp = await request.get(`${API_URL}/role/${encodeURIComponent(name)}/permissions`, {
    headers: headers(token),
  });
  if (permsResp.ok()) {
    const perms = await permsResp.json() as Array<{ name: string }>;
    if (perms.length > 0) {
      await request.delete(`${API_URL}/role/${encodeURIComponent(name)}/permissions`, {
        data: perms.map(p => p.name),
        headers: headers(token),
      });
    }
  }
  await request.delete(`${API_URL}/role/${encodeURIComponent(name)}`, {
    headers: headers(token),
  });
}

/**
 * Delete an OIDC application via API (cleanup).
 */
export async function deleteOidcApplication(
  request: APIRequestContext,
  token: string,
  name: string,
): Promise<void> {
  await request.delete(`${API_URL}/oidc_application/${encodeURIComponent(name)}`, {
    headers: headers(token),
  });
}

/**
 * Get a group by name (returns the group with id).
 */
export async function getGroup(
  request: APIRequestContext,
  token: string,
  name: string,
): Promise<{ id: number; name: string; description: string }> {
  const response = await request.get(`${API_URL}/group/${encodeURIComponent(name)}`, {
    headers: headers(token),
  });
  if (!response.ok()) {
    throw new Error(`Get group ${name} failed: ${response.status()}`);
  }
  return response.json();
}

/**
 * Get a role by name (returns the role with id).
 */
export async function getRole(
  request: APIRequestContext,
  token: string,
  name: string,
): Promise<{ id: number; name: string; description: string }> {
  const response = await request.get(`${API_URL}/role/${encodeURIComponent(name)}`, {
    headers: headers(token),
  });
  if (!response.ok()) {
    throw new Error(`Get role ${name} failed: ${response.status()}`);
  }
  return response.json();
}

/**
 * Create an OIDC application via API.
 */
export async function createOidcApplication(
  request: APIRequestContext,
  token: string,
  data: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  const response = await request.post(`${API_URL}/oidc_applications`, {
    data,
    headers: headers(token),
  });
  if (!response.ok()) {
    const text = await response.text();
    throw new Error(`Create OIDC application failed: ${response.status()} ${text}`);
  }
  return response.json();
}
