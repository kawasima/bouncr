import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { BouncrWorld } from '../support/world';
import {
  BASE_URL,
  ADMIN_ACCOUNT,
  ADMIN_PASSWORD,
  TEST_USERS,
} from '../support/config';
import { signInViaApi } from '../support/auth.helper';

/**
 * Resolves account name to credentials.
 * "admin" uses the real admin account; test user names map to TEST_USERS.
 */
function resolveCredentials(account: string): { account: string; password: string } {
  if (account === 'admin') {
    return { account: ADMIN_ACCOUNT, password: ADMIN_PASSWORD };
  }
  const testUser = TEST_USERS[account as keyof typeof TEST_USERS];
  if (testUser) {
    return { account: testUser.account, password: testUser.password };
  }
  throw new Error(`Unknown test account: ${account}`);
}

Given('I am on the sign-in page', async function (this: BouncrWorld) {
  await this.page.goto(`${BASE_URL}/sign_in`);
  await this.page.waitForSelector('#account');
});

Given('I am signed in as {string}', async function (this: BouncrWorld, account: string) {
  const creds = resolveCredentials(account);
  const token = await signInViaApi(this.request, creds.account, creds.password);
  // Set the BOUNCR_TOKEN cookie in the browser context so the UI recognizes the session
  await this.context.addCookies([{
    name: 'BOUNCR_TOKEN',
    value: token,
    url: BASE_URL,
  }]);
  // Also store the account name in localStorage so auth-context picks it up
  await this.page.goto(BASE_URL);
  await this.page.evaluate((acct: string) => {
    localStorage.setItem('bouncr_account', acct);
  }, creds.account);
  // Reload to apply the auth state
  await this.page.reload();
  // Wait for the home page to load (user account visible in navbar)
  await this.page.waitForSelector('text=Sign Out', { timeout: 15_000 });
});

Given('I am on the admin {string} page', async function (this: BouncrWorld, adminPage: string) {
  await this.page.goto(`${BASE_URL}/admin/${adminPage}`);
  await this.page.waitForLoadState('networkidle');
});

When('I navigate to {string}', async function (this: BouncrWorld, path: string) {
  await this.page.goto(`${BASE_URL}${path}`);
  await this.page.waitForLoadState('networkidle');
});

When('I click {string}', async function (this: BouncrWorld, buttonText: string) {
  // Try button role first, then general text click
  const button = this.page.getByRole('button', { name: buttonText });
  if (await button.count() > 0) {
    await button.first().click();
  } else {
    // May be a link
    const link = this.page.getByRole('link', { name: buttonText });
    if (await link.count() > 0) {
      await link.first().click();
    } else {
      // Fallback to text locator
      await this.page.locator(`text="${buttonText}"`).first().click();
    }
  }
});

When('I fill in {string} with {string}', async function (this: BouncrWorld, fieldId: string, value: string) {
  const input = this.page.locator(`#${fieldId}`);
  await input.fill(value);
});

When('I click on {string} in the list', async function (this: BouncrWorld, itemText: string) {
  // DataTable rows are clickable; find the row containing the text and click it
  const row = this.page.locator('tr', { hasText: itemText }).first();
  await row.click();
  await this.page.waitForLoadState('networkidle');
});

When('I click {string} and confirm', async function (this: BouncrWorld, buttonText: string) {
  // Click the initial delete/action button
  await this.page.getByRole('button', { name: buttonText }).first().click();
  // Wait for the confirmation button to appear, then click it
  await this.page.waitForSelector('button:has-text("Confirm")', { timeout: 5_000 });
  await this.page.getByRole('button', { name: 'Confirm' }).click();
  await this.page.waitForLoadState('networkidle');
});

Then('I should see {string}', async function (this: BouncrWorld, text: string) {
  await expect(this.page.locator(`text="${text}"`).first()).toBeVisible({ timeout: 10_000 });
});

Then('I should see a success indication', async function (this: BouncrWorld) {
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

Then('I should return to the list view', async function (this: BouncrWorld) {
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
