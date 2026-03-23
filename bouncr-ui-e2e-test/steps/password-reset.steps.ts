import { Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { BouncrWorld } from '../support/world';

Then('I should see the reset code sent message', async function (this: BouncrWorld) {
  // The ResetChallengePage shows "A password reset code has been dispatched" on success
  await expect(
    this.page.locator('text=A password reset code has been dispatched'),
  ).toBeVisible({ timeout: 10_000 });
});
