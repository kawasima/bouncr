import { Given, After } from '@cucumber/cucumber';
import { BouncrWorld } from '../support/world';
import {
  createUser,
  createPasswordCredential,
  deleteUser,
} from '../support/auth.helper';

/**
 * Creates a test user via API as a prerequisite for scenarios.
 * Tracks created users for cleanup in the After hook.
 */
Given('a user {string} exists', async function (this: BouncrWorld, account: string) {
  try {
    await createUser(this.request, this.adminToken, account, `Test ${account}`, `${account}@example.com`);
  } catch {
    // User may already exist
  }
  try {
    await createPasswordCredential(this.request, this.adminToken, account, 'TestP@ss123');
  } catch {
    // Credential may already exist
  }
  // Track for cleanup
  const cleanupUsers = (this.testData.get('cleanupUsers') as string[]) ?? [];
  cleanupUsers.push(account);
  this.testData.set('cleanupUsers', cleanupUsers);
});

After({ tags: '@cleanup_users' }, async function (this: BouncrWorld) {
  // This hook is not tag-based; we'll do cleanup via the general After.
});

// General cleanup for scenario-specific test data
After(async function (this: BouncrWorld) {
  const cleanupUsers = (this.testData.get('cleanupUsers') as string[]) ?? [];
  for (const account of cleanupUsers) {
    await deleteUser(this.request, this.adminToken, account);
  }
  const cleanupGroups = (this.testData.get('cleanupGroups') as string[]) ?? [];
  for (const name of cleanupGroups) {
    const { deleteGroup } = await import('../support/auth.helper');
    await deleteGroup(this.request, this.adminToken, name);
  }
  const cleanupRoles = (this.testData.get('cleanupRoles') as string[]) ?? [];
  for (const name of cleanupRoles) {
    const { deleteRole } = await import('../support/auth.helper');
    await deleteRole(this.request, this.adminToken, name);
  }
  const cleanupOidcApps = (this.testData.get('cleanupOidcApps') as string[]) ?? [];
  for (const name of cleanupOidcApps) {
    const { deleteOidcApplication } = await import('../support/auth.helper');
    await deleteOidcApplication(this.request, this.adminToken, name);
  }
});
