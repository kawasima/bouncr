import { Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { BouncrWorld } from '../support/world';

// The invitation page uses "Create" as the submit button text,
// and shows "Invitation Created" on success. Both are handled by common steps.
// This file exists for any invitation-specific steps.

Then('I should see {string} in the invitations list', async function (this: BouncrWorld, text: string) {
  await expect(this.page.locator(`text="${text}"`)).toBeVisible({ timeout: 10_000 });
});
