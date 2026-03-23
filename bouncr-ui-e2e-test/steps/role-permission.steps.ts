import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { BouncrWorld } from '../support/world';
import { createRole } from '../support/auth.helper';

Given('a role {string} exists', async function (this: BouncrWorld, roleName: string) {
  try {
    await createRole(this.request, this.adminToken, roleName, `E2E test role ${roleName}`);
  } catch {
    // May already exist
  }
  // Track for cleanup
  const cleanupRoles = (this.testData.get('cleanupRoles') as string[]) ?? [];
  cleanupRoles.push(roleName);
  this.testData.set('cleanupRoles', cleanupRoles);
});

When('I check permission {string}', async function (this: BouncrWorld, permissionName: string) {
  // Permissions are rendered as labeled checkboxes under the "Permissions" section
  const permLabel = this.page.locator('label', { hasText: permissionName }).first();
  const checkbox = permLabel.locator('input[type="checkbox"]');
  if (!(await checkbox.isChecked())) {
    await checkbox.check();
  }
});

Then('the permission {string} should be checked', async function (this: BouncrWorld, permissionName: string) {
  const permLabel = this.page.locator('label', { hasText: permissionName }).first();
  const checkbox = permLabel.locator('input[type="checkbox"]');
  await expect(checkbox).toBeChecked({ timeout: 10_000 });
});
