import { When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { BouncrWorld } from '../support/world';
import { BASE_URL } from '../support/config';

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
});

Then('I should see the home page', async function (this: BouncrWorld) {
  await this.page.waitForURL(`${BASE_URL}/`, { timeout: 15_000 });
  // The home page shows the "Profile" heading
  await expect(this.page.locator('text=Profile')).toBeVisible({ timeout: 10_000 });
});

Then('I should see an authentication error', async function (this: BouncrWorld) {
  // The ProblemAlert component renders an error with role="alert"
  await expect(this.page.locator('[role="alert"]')).toBeVisible({ timeout: 10_000 });
});

Then('I should be on the sign-in page', async function (this: BouncrWorld) {
  await this.page.waitForURL(`${BASE_URL}/sign_in`, { timeout: 10_000 });
  await expect(this.page.locator('h1:has-text("Sign In")')).toBeVisible();
});
