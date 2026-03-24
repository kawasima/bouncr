import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { BouncrWorld } from '../support/world';
import {
  BASE_URL,
  ADMIN_ACCOUNT,
  TEST_USERS,
} from '../support/config';
import { signInViaApi } from '../support/auth.helper';

Given('I am on the sign-in page', async function (this: BouncrWorld) {
  await this.page.goto(`${BASE_URL}/sign_in`);
  await this.page.waitForSelector('#account');
});

Given('I am signed in as {string}', { timeout: 30_000 }, async function (this: BouncrWorld, account: string) {
  let acct: string;
  let token: string | undefined;

  if (account === 'admin') {
    acct = ADMIN_ACCOUNT;
    token = this.cachedTokens['admin'];
  } else {
    const testUser = TEST_USERS[account as keyof typeof TEST_USERS];
    if (!testUser) throw new Error(`Unknown test account: ${account}`);
    acct = testUser.account;
    token = this.cachedTokens[account];
  }

  // Fallback to API sign-in if no cached token
  if (!token) {
    const password = account === 'admin'
      ? this.adminPassword
      : TEST_USERS[account as keyof typeof TEST_USERS]?.password;
    if (!password) throw new Error(`No password for ${account}`);
    const result = await signInViaApi(this.request, acct, password);
    token = result.token;
    const cacheKey = account === 'admin' ? 'admin' : account;
    this.cachedTokens[cacheKey] = token;
    // Also update adminToken for helper functions
    if (account === 'admin') {
      this.adminToken = token;
    }
  }

  await this.context.addCookies([{
    name: 'BOUNCR_TOKEN',
    value: token,
    url: BASE_URL,
  }]);
  // Navigate to sign-in page to initialize the app, then set localStorage and navigate home
  await this.page.goto(`${BASE_URL}/sign_in`);
  await this.page.evaluate((a: string) => {
    localStorage.setItem('bouncr_session', JSON.stringify({ account: a }));
  }, acct);
  // Navigate to home with localStorage set so RequireAuth passes
  await this.page.goto(BASE_URL);
  await this.page.waitForSelector('text=Sign Out', { timeout: 15_000 });
});

Given('I am on the admin {string} page', { timeout: 30_000 }, async function (this: BouncrWorld, adminPage: string) {
  await this.page.goto(`${BASE_URL}/admin/${adminPage}`);
  await this.page.waitForLoadState('networkidle');
  // Wait for loading spinner to disappear (permissions/data loaded)
  await this.page.locator('.animate-spin').waitFor({ state: 'detached', timeout: 10_000 }).catch(() => {});
});

When('I navigate to {string}', { timeout: 30_000 }, async function (this: BouncrWorld, path: string) {
  await this.page.goto(`${BASE_URL}${path}`);
  await this.page.waitForLoadState('networkidle');
});

When('I click {string}', { timeout: 30_000 }, async function (this: BouncrWorld, buttonText: string) {
  // Try button first (with wait), then link, then text
  const button = this.page.getByRole('button', { name: buttonText }).first();
  const buttonVisible = await button.isVisible().catch(() => false);

  if (!buttonVisible) {
    // Wait for button to appear (up to 10s)
    await button.waitFor({ state: 'visible', timeout: 10_000 }).catch(() => {});
  }

  if (await button.isVisible().catch(() => false)) {
    await button.click();
    return;
  }

  // Try link
  const link = this.page.getByRole('link', { name: buttonText }).first();
  if (await link.isVisible().catch(() => false)) {
    await link.click();
    return;
  }

  // Fallback: text
  await this.page.locator(`text="${buttonText}"`).first().click({ timeout: 10_000 });
});

When('I fill in {string} with {string}', async function (this: BouncrWorld, fieldId: string, value: string) {
  const input = this.page.locator(`#${fieldId}`);
  await input.fill(value);
});

When('I click on {string} in the list', { timeout: 30_000 }, async function (this: BouncrWorld, itemText: string) {
  // DataTable rows are clickable; wait for the row to appear then click it
  const row = this.page.locator('tr', { hasText: itemText }).first();
  await row.waitFor({ state: 'visible', timeout: 15_000 });
  await row.click();
  await this.page.waitForLoadState('networkidle');
});

When('I click {string} and confirm', { timeout: 30_000 }, async function (this: BouncrWorld, buttonText: string) {
  // Click the initial delete/action button
  const btn = this.page.getByRole('button', { name: buttonText }).first();
  await btn.waitFor({ state: 'visible', timeout: 15_000 });
  await btn.click();
  // Wait for the confirmation button to appear, then click it
  await this.page.waitForSelector('button:has-text("Confirm")', { timeout: 10_000 });
  await this.page.getByRole('button', { name: 'Confirm' }).click();
  await this.page.waitForLoadState('networkidle');
});

Then('I should see {string}', { timeout: 15_000 }, async function (this: BouncrWorld, text: string) {
  await expect(this.page.locator(`text="${text}"`).first()).toBeVisible({ timeout: 10_000 });
});

Then('I should see a success indication', { timeout: 15_000 }, async function (this: BouncrWorld) {
  // After saving, the form typically switches back to the list view or shows the edit form
  // with the saved data. We check that no error is displayed and the page loaded.
  await this.page.waitForLoadState('networkidle');
  // Ensure no problem alert is visible
  const problemAlert = this.page.locator('[role="alert"]');
  if (await problemAlert.count() > 0) {
    const text = await problemAlert.first().textContent();
    if (text && text.toLowerCase().includes('error')) {
      throw new Error(`Unexpected error displayed: ${text}`);
    }
  }
});

Then('I should return to the list view', { timeout: 15_000 }, async function (this: BouncrWorld) {
  // After delete + confirm, we return to the list view which has a "New" button
  await this.page.waitForLoadState('networkidle');
  // The list view shows a heading and possibly a "New" button
  const heading = this.page.locator('h2');
  await expect(heading.first()).toBeVisible({ timeout: 10_000 });
});

Then('I should not see the {string} button', async function (this: BouncrWorld, buttonText: string) {
  const button = this.page.getByRole('button', { name: buttonText });
  await expect(button).toHaveCount(0);
});
