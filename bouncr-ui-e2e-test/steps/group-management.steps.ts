import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { BouncrWorld } from '../support/world';
import { createGroup, createUser } from '../support/auth.helper';

Given('a group {string} exists', async function (this: BouncrWorld, groupName: string) {
  try {
    await createGroup(this.request, this.adminToken, groupName, `E2E test group ${groupName}`);
  } catch {
    // May already exist
  }
  // Track for cleanup
  const cleanupGroups = (this.testData.get('cleanupGroups') as string[]) ?? [];
  cleanupGroups.push(groupName);
  this.testData.set('cleanupGroups', cleanupGroups);
});

When('I search for user {string}', async function (this: BouncrWorld, userAccount: string) {
  // The group edit page has a "Search users to add..." input
  const searchInput = this.page.locator('input[placeholder="Search users to add..."]');
  await searchInput.fill(userAccount);
  // Click the Search button next to it
  await this.page.getByRole('button', { name: 'Search' }).click();
  await this.page.waitForLoadState('networkidle');
});

When('I click on the search result {string}', async function (this: BouncrWorld, userAccount: string) {
  // Search results are buttons showing the user account in a <span class="text-gold">
  const resultButton = this.page.locator('button', { hasText: userAccount });
  await resultButton.first().click();
  await this.page.waitForLoadState('networkidle');
});

Then('I should see {string} in the group members', { timeout: 15_000 }, async function (this: BouncrWorld, userAccount: string) {
  // The group members table shows accounts in <td> elements
  const membersTable = this.page.locator('h3:has-text("Users in Group")').locator('..').locator('table');
  await expect(membersTable.locator(`td:has-text("${userAccount}")`)).toBeVisible({ timeout: 10_000 });
});
