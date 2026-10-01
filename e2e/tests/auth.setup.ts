import { test as setup } from "@playwright/test";
import { expectSignedIn, signIn, STORAGE_STATE } from "../support/auth";

setup("sign in once and save the session", async ({ page }) => {
  await signIn(page);
  await expectSignedIn(page);
  await page.context().storageState({ path: STORAGE_STATE });
});
