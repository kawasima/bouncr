import { World, setWorldConstructor } from '@cucumber/cucumber';
import type { Page, BrowserContext, Browser, APIRequestContext } from '@playwright/test';

export class BouncrWorld extends World {
  page!: Page;
  context!: BrowserContext;
  browser!: Browser;
  request!: APIRequestContext;
  adminToken!: string;
  adminPassword!: string;
  cachedTokens: Record<string, string> = {};
  testData: Map<string, unknown> = new Map();
}

setWorldConstructor(BouncrWorld);
