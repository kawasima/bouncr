import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { BouncrWorld } from '../support/world';
import { BASE_URL } from '../support/config';
import { createUser, createPasswordCredential, deleteUser } from '../support/auth.helper';

Given('a user {string} exists with initial password {string}', async function (this: BouncrWorld, account: string, password: string) {
  // Clean up any leftover user from previous runs
  await deleteUser(this.request, this.adminToken, account);
  await createUser(this.request, this.adminToken, account, account, `${account}@example.com`);
  await createPasswordCredential(this.request, this.adminToken, account, password);
  this.testData.set('initUser', account);
});

When('I enter account {string} and the admin password', async function (this: BouncrWorld, account: string) {
  await this.page.locator('#account').fill(account);
  await this.page.locator('#password').fill(this.adminPassword);
});

When('I enter account {string} and password {string}', async function (this: BouncrWorld, account: string, password: string) {
  await this.page.locator('#account').fill(account);
  await this.page.locator('#password').fill(password);
});

When('I click the sign-in button', async function (this: BouncrWorld) {
  await this.page.getByRole('button', { name: /enter/i }).click();
});

When('I click the sign-out button', async function (this: BouncrWorld) {
  await this.page.getByRole('button', { name: /sign out/i }).click();
  // Invalidate the cached admin token so next sign-in forces a fresh one
  delete this.cachedTokens['admin'];
});

Then('I should see the home page', { timeout: 20_000 }, async function (this: BouncrWorld) {
  await this.page.waitForURL(`${BASE_URL}/`, { timeout: 15_000 });
  // The home page shows the "Profile" heading
  await expect(this.page.locator('h2:has-text("Profile")')).toBeVisible({ timeout: 10_000 });
});

Then('I should see an authentication error', { timeout: 15_000 }, async function (this: BouncrWorld) {
  // The ProblemAlert component renders a div containing "Error" in uppercase
  await expect(
    this.page.locator('p', { hasText: 'Error' }).first(),
  ).toBeVisible({ timeout: 10_000 });
});

Then('I should be on the sign-in page', { timeout: 15_000 }, async function (this: BouncrWorld) {
  await this.page.waitForURL(`${BASE_URL}/sign_in`, { timeout: 10_000 });
  await expect(this.page.locator('h1:has-text("Sign In")')).toBeVisible();
});

Then('I should be on the change password page', async function (this: BouncrWorld) {
  await this.page.waitForURL(`${BASE_URL}/change_password`, { timeout: 10_000 });
});

When('I click the confirm button', async function (this: BouncrWorld) {
  await this.page.getByRole('button', { name: /confirm/i }).click();
  // Wait for the form submission to complete before cleanup
  await this.page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => {});

  // Cleanup: delete the init user after password change test
  const initUser = this.testData.get('initUser') as string | undefined;
  if (initUser) {
    await deleteUser(this.request, this.adminToken, initUser);
    this.testData.delete('initUser');
  }
});
