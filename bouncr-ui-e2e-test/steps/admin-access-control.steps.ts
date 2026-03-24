import { Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { BouncrWorld } from '../support/world';

Then('I should see admin menu items:', { timeout: 30_000 }, async function (this: BouncrWorld, dataTable: { raw: () => string[][] }) {
  const expectedItems = dataTable.raw()[0];
  // The admin menu is rendered inside a <nav> under the "Administration" heading
  const adminNav = this.page.locator('nav', {
    has: this.page.locator('a'),
  }).last();

  for (const item of expectedItems) {
    await expect(
      this.page.getByRole('link', { name: item, exact: true }),
    ).toBeVisible({ timeout: 10_000 });
  }
});

Then('I should not see the admin menu', async function (this: BouncrWorld) {
  // The admin menu shows "Administration" heading when visible
  const administrationHeading = this.page.locator('text=Administration');
  await expect(administrationHeading).toHaveCount(0);
});

Then('I should not see admin menu item {string}', async function (this: BouncrWorld, itemName: string) {
  // Wait a moment for potential lazy loading
  await this.page.waitForLoadState('networkidle');
  // Check that the link for this admin item is not present in the sidebar
  const link = this.page.locator('nav a', { hasText: itemName });
  await expect(link).toHaveCount(0);
});

Then('form inputs should be disabled', async function (this: BouncrWorld) {
  await this.page.waitForLoadState('networkidle');
  // On a read-only admin page, when we click a row to edit, inputs are disabled.
  // First click a row if there are any
  const rows = this.page.locator('tbody tr');
  if (await rows.count() > 0) {
    await rows.first().click();
    await this.page.waitForLoadState('networkidle');
    // Check that form inputs are disabled
    const inputs = this.page.locator('form input:not([type="hidden"])');
    const count = await inputs.count();
    for (let i = 0; i < count; i++) {
      await expect(inputs.nth(i)).toBeDisabled();
    }
  }
});
