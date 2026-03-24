import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { BouncrWorld } from '../support/world';
import { createOidcApplication, deleteOidcApplication } from '../support/auth.helper';

Given('no OIDC application {string} exists', async function (this: BouncrWorld, name: string) {
  await deleteOidcApplication(this.request, this.adminToken, name);
});

Given('an OIDC application {string} exists', async function (this: BouncrWorld, name: string) {
  try {
    await createOidcApplication(this.request, this.adminToken, {
      name,
      grant_types: ['client_credentials'],
      description: `E2E test app ${name}`,
    });
  } catch {
    // May already exist
  }
  // Track for cleanup
  const cleanupOidcApps = (this.testData.get('cleanupOidcApps') as string[]) ?? [];
  cleanupOidcApps.push(name);
  this.testData.set('cleanupOidcApps', cleanupOidcApps);
});

When('I check {string} grant type', async function (this: BouncrWorld, grantLabel: string) {
  const checkbox = this.page.locator('label', { hasText: grantLabel }).locator('input[type="checkbox"]');
  if (!(await checkbox.isChecked())) {
    await checkbox.check();
  }
});

When('I uncheck {string} grant type', async function (this: BouncrWorld, grantLabel: string) {
  const checkbox = this.page.locator('label', { hasText: grantLabel }).locator('input[type="checkbox"]');
  if (await checkbox.isChecked()) {
    await checkbox.uncheck();
  }
});

Then('I should see client credentials', { timeout: 15_000 }, async function (this: BouncrWorld) {
  // After creating an OIDC app, the "Application Created" card is shown
  await expect(this.page.locator('text=Application Created')).toBeVisible({ timeout: 10_000 });
  // Track the created app for cleanup
  const cleanupOidcApps = (this.testData.get('cleanupOidcApps') as string[]) ?? [];
  cleanupOidcApps.push('e2e_cli_app');
  this.testData.set('cleanupOidcApps', cleanupOidcApps);
});

Then('the client ID should not be empty', async function (this: BouncrWorld) {
  // The client ID is shown in a <code> block after "Client ID"
  const clientIdCode = this.page.locator('text=Client ID').locator('..').locator('code');
  const clientId = await clientIdCode.textContent();
  expect(clientId).toBeTruthy();
  expect(clientId!.trim().length).toBeGreaterThan(0);
});

Then('the client secret should not be empty', async function (this: BouncrWorld) {
  const clientSecretCode = this.page.locator('code.text-gold');
  const clientSecret = await clientSecretCode.textContent();
  expect(clientSecret).toBeTruthy();
  expect(clientSecret!.trim().length).toBeGreaterThan(0);
});

Then('I should see a new client secret', { timeout: 15_000 }, async function (this: BouncrWorld) {
  // After regeneration, "New secret generated" text appears along with a <code> element
  await expect(this.page.locator('text=New secret generated')).toBeVisible({ timeout: 10_000 });
  const code = this.page.locator('code.select-all, code').last();
  const secret = await code.textContent();
  expect(secret).toBeTruthy();
  expect(secret!.trim().length).toBeGreaterThan(0);
});
